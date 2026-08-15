package com.aozijx.passly.data.repository.attachment

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentResourceGarbageCollector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val telemetry: TelemetryReporter,
) {
    private val mutationMutex = Mutex()

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
    suspend fun drain(limit: Int = 64) = withMutationLock {
        val pending = databaseSession.query {
            attachmentResourceDao().getPendingGc(limit)
        }
        pending.forEach { item -> drainOne(item) }
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
            runCatching {
                val file = resourceFile(item.resourceId)
                !file.exists() || file.delete()
            }.getOrDefault(false)
        }
        if (!deleted) {
            databaseSession.transaction {
                attachmentResourceDao().recordAttempt(item.resourceId, System.currentTimeMillis())
            }
            telemetry.report(
                EventLevel.WARN,
                EventCategory.FILE_IO,
                "attachment.resource_gc_failed"
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
