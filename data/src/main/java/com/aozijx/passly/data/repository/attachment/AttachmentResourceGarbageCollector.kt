package com.aozijx.passly.data.repository.attachment

import android.content.Context
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
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

@Singleton
class AttachmentResourceGarbageCollector
    @Inject
    constructor(
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
                    drainAfterUnlock()
                }
            }
        }

        suspend fun <T> withMutationLock(block: suspend () -> T): T = mutationMutex.withLock { block() }

        /** Phase one: tombstone unreferenced resources in the same database transaction. */
        suspend fun scheduleInTransaction(db: AppDatabase) =
            with(db) {
                val resources =
                    attachmentResourceDao()
                        .getUnreferenced()
                        .filter { it.lifecycleState == AttachmentResourceState.ACTIVE }
                if (resources.isEmpty()) return@with
                val ids = resources.map { it.resourceId }
                check(
                    attachmentResourceDao().markPendingGc(
                        ids,
                        AttachmentResourceState.PENDING_GC,
                        System.currentTimeMillis(),
                    ) == ids.size,
                )
            }

        suspend fun reactivateInTransaction(
            db: AppDatabase,
            resourceId: String,
        ) = with(db) {
            check(
                attachmentResourceDao().reactivate(resourceId, AttachmentResourceState.ACTIVE) == 1,
            )
        }

        /** Phase two: idempotent file deletion followed by database finalization. */
        suspend fun drain(limit: Int = MAX_BATCH_SIZE) {
            val result =
                try {
                    drainPass(limit)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (access.hasFullSecureSessionAccess()) wakeups.trySend(Unit)
                    throw failure
                }
            if (result != AttachmentGcPassResult.COMPLETE && access.hasFullSecureSessionAccess()) {
                wakeups.trySend(Unit)
            }
        }

        private suspend fun drainPass(limit: Int = MAX_BATCH_SIZE): AttachmentGcPassResult {
            require(limit > 0)
            val batchSize = limit.coerceAtMost(MAX_BATCH_SIZE)
            return logger.inSpan(Operations.ResourceCleanup) {
                withMutationLock {
                    drainAttachmentGcPass(
                        batchSize = batchSize,
                        access = access,
                        onItemFailure = { logger.log(LogStatements.Application.resourceCleanupFailed(it)) },
                        loadBatch = { afterEnqueuedAt, afterResourceId ->
                            databaseSession.queryFullVault(
                                onDenied = { emptyList() },
                            ) {
                                if (afterEnqueuedAt == null || afterResourceId == null) {
                                    attachmentResourceDao().getPendingGc(batchSize)
                                } else {
                                    attachmentResourceDao().getPendingGcAfter(
                                        limit = batchSize,
                                        afterEnqueuedAt = afterEnqueuedAt,
                                        afterResourceId = afterResourceId,
                                    )
                                }
                            }
                        },
                        drainOne = ::drainOne,
                    )
                }
            }
        }

        private suspend fun drainAfterUnlock() {
            runAttachmentGcDrain(
                access = access,
                drainPass = { drainPass() },
                onFailure = { logger.log(LogStatements.Application.resourceCleanupFailed(it)) },
            )
        }

        private suspend fun drainOne(item: AttachmentResourceEntity): Boolean {
            if (!access.hasFullSecureSessionAccess()) return false
            val stillUnreferenced =
                databaseSession.transactionFullVault(
                    onDenied = { false },
                ) {
                    val hasRefs =
                        attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
                            attachmentResourceDao().revisionRefCount(item.resourceId) > 0
                    if (hasRefs) {
                        attachmentResourceDao().reactivate(
                            item.resourceId,
                            AttachmentResourceState.ACTIVE,
                        )
                    }
                    !hasRefs
                }
            if (!stillUnreferenced) return true
            if (!access.hasFullSecureSessionAccess()) return false

            val deleted =
                withContext(Dispatchers.IO) {
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
                if (!access.hasFullSecureSessionAccess()) return false
                databaseSession.transactionFullVault(
                    onDenied = { Unit },
                ) {
                    attachmentResourceDao().recordAttempt(item.resourceId, System.currentTimeMillis())
                }
                logger.log(
                    LogStatements.Application.resourceCleanupFailed(
                        IOException("Attachment resource deletion failed"),
                    ),
                )
                return false
            }

            if (!access.hasFullSecureSessionAccess()) return false
            databaseSession.transactionFullVault(
                onDenied = { Unit },
            ) {
                val hasRefs =
                    attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
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
            return true
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
            const val MAX_BATCH_SIZE = 64
            val KEYED_ID = Regex("[a-f0-9]{64}")
        }
    }
