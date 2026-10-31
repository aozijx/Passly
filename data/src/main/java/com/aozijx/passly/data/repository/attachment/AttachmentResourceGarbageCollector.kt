package com.aozijx.passly.data.repository.attachment

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentResourceGarbageCollector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val access: SecureSessionAccessState,
    private val logger: Logger,
) {
    private val mutationMutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wakeups = Channel<Unit>(Channel.CONFLATED)

    init {
        cleanupScope.launch {
            access.authenticationState.collect { wakeups.trySend(Unit) }
        }
        cleanupScope.launch {
            databaseSession.lockStateFlow.collect { wakeups.trySend(Unit) }
        }
        cleanupScope.launch {
            for (ignored in wakeups) {
                if (!access.hasFullSecureSessionAccess()) continue
                cleanupScope.launch { drainAfterUnlock() }.join()
            }
        }
    }

    suspend fun <T> withMutationLock(block: suspend () -> T): T = mutationMutex.withLock { block() }

    /** Phase one: tombstone unreferenced resources in the same database transaction. */
    suspend fun scheduleInTransaction(db: AppDatabase) = with(db) {
        val resources = attachmentResourceDao().getUnreferenced()
            .filter { it.lifecycleState == AttachmentResourceState.ACTIVE }
        if (resources.isEmpty()) return@with
        val ids = resources.map { it.resourceId }
        check(
            attachmentResourceDao().markPendingGc(
                ids,
                AttachmentResourceState.PENDING_GC,
                System.currentTimeMillis(),
            ) == ids.size
        )
    }

    suspend fun reactivateInTransaction(db: AppDatabase, resourceId: String) = with(db) {
        check(
            attachmentResourceDao().reactivate(resourceId, AttachmentResourceState.ACTIVE) == 1
        )
    }

    /** Phase two: idempotent file deletion followed by database finalization. */
    suspend fun drain(limit: Int = 64) = logger.inSpan(Operations.ResourceCleanup) {
        withMutationLock {
            val pending = databaseSession.query {
                attachmentResourceDao().getPendingGc(limit)
            }
            pending.forEach { item -> drainOne(item) }
        }
    }

    private suspend fun drainAfterUnlock() {
        try {
            drain()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.log(LogStatements.Application.resourceCleanupFailed(error))
        }
    }

    private suspend fun drainOne(item: AttachmentResourceEntity) {
        val stillUnreferenced = databaseSession.transaction {
            val hasRefs = attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
                attachmentResourceDao().revisionRefCount(item.resourceId) > 0
            if (hasRefs) {
                attachmentResourceDao().reactivate(
                    item.resourceId,
                    AttachmentResourceState.ACTIVE,
                )
            }
            !hasRefs
        }
        if (!stillUnreferenced) return

        val deleted = withContext(Dispatchers.IO) {
            try {
                val file = resourceFile(item.resourceId)
                !file.exists() || file.delete()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
        if (!deleted) {
            databaseSession.transaction {
                attachmentResourceDao().recordAttempt(item.resourceId, System.currentTimeMillis())
            }
            logger.log(
                LogStatements.Application.resourceCleanupFailed(
                    IOException("Attachment resource deletion failed"),
                ),
            )
            return
        }

        databaseSession.transaction {
            val hasRefs = attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
                attachmentResourceDao().revisionRefCount(item.resourceId) > 0
            if (hasRefs) {
                attachmentResourceDao().reactivate(
                    item.resourceId,
                    AttachmentResourceState.ACTIVE,
                )
            } else {
                attachmentResourceDao().deletePending(item.resourceId)
            }
        }
    }

    fun resourceFile(resourceId: String): File {
        require(resourceId.matches(KEYED_ID)) { "Invalid attachment resource ID" }
        val root = VaultResourcePaths.attachmentDir(context).canonicalFile
        val contentRoot = File(root, CONTENT_DIRECTORY).canonicalFile
        val target = File(contentRoot, "$resourceId.enc").canonicalFile
        require(target.parentFile == contentRoot) { "Attachment content path escaped its root" }
        return target
    }

    private companion object {
        const val CONTENT_DIRECTORY = "content"
        val KEYED_ID = Regex("[a-f0-9]{64}")
    }
}
