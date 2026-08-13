package com.aozijx.passly.data.repository.attachment

import android.content.Context
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.session.UnifiedSessionManager
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.AttachmentGcQueueEntity
import com.aozijx.passly.data.model.entity.AttachmentResourceState
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
    private val sessionManager: UnifiedSessionManager,
) {
    private val mutationMutex = Mutex()

    suspend fun <T> withMutationLock(block: suspend () -> T): T = mutationMutex.withLock { block() }

    /** Phase one: tombstone resources and persist work in the same database transaction. */
    suspend fun scheduleInTransaction(db: AppDatabase) = with(db) {
        val resources = attachmentResourceDao().getUnreferenced()
            .filter { it.lifecycleState == AttachmentResourceState.ACTIVE }
        if (resources.isEmpty()) return@with
        val ids = resources.map { it.resourceId }
        check(attachmentResourceDao().updateState(ids, AttachmentResourceState.PENDING_GC) == ids.size)
        attachmentGcQueueDao().enqueue(
            ids.map { AttachmentGcQueueEntity(it, System.currentTimeMillis()) }
        )
    }

    suspend fun reactivateInTransaction(db: AppDatabase, resourceId: String) = with(db) {
        attachmentGcQueueDao().delete(resourceId)
        check(attachmentResourceDao().updateState(resourceId, AttachmentResourceState.ACTIVE) == 1)
    }

    /** Phase two: idempotent file deletion followed by database finalization. */
    suspend fun drain(limit: Int = 64) = withMutationLock {
        val pending = sessionManager.query { attachmentGcQueueDao().getPending(limit) }
        pending.forEach { item -> drainOne(item) }
    }

    private suspend fun drainOne(item: AttachmentGcQueueEntity) {
        val stillUnreferenced = sessionManager.transaction {
            val hasRefs = attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
                attachmentResourceDao().revisionRefCount(item.resourceId) > 0
            if (hasRefs) {
                attachmentResourceDao().updateState(item.resourceId, AttachmentResourceState.ACTIVE)
                attachmentGcQueueDao().delete(item.resourceId)
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
            sessionManager.transaction {
                attachmentGcQueueDao().recordAttempt(item.resourceId, System.currentTimeMillis())
            }
            AppTelemetry.w(EventCategory.FILE_IO, "attachment.resource_gc_failed")
            return
        }

        sessionManager.transaction {
            val hasRefs = attachmentResourceDao().currentRefCount(item.resourceId) > 0 ||
                attachmentResourceDao().revisionRefCount(item.resourceId) > 0
            if (hasRefs) {
                attachmentResourceDao().updateState(item.resourceId, AttachmentResourceState.ACTIVE)
                attachmentGcQueueDao().delete(item.resourceId)
            } else {
                attachmentGcQueueDao().delete(item.resourceId)
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
