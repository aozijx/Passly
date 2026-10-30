package com.aozijx.passly.data.repository.entry.command

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.domain.entry.port.EntryIconResourceCleaner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

internal data class DeletedEntryResources(
    val customIconPath: String?,
    val revisionIconPaths: List<String> = emptyList(),
)

/**
 * 清理条目永久删除后留下的文件资源。
 *
 * 数据库删除优先提交，文件清理随后进行。附件内容本身已加密；若文件系统删除失败，
 * 这里只记录结构化诊断事件，不把已经成功的数据库删除伪装成失败。
 */
@Singleton
internal class EntryResourceCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val databaseTransactions: DatabaseTransactionRunner,
    private val logger: Logger,
    access: SecureSessionAccessState,
    databaseSession: AppDatabaseSession,
) : EntryIconResourceCleaner {
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cleanupQueue = IconResourceCleanupQueue(cleanupScope, access, databaseSession) { path ->
        logger.inSpan(Operations.ResourceCleanup) {
            attachmentGarbageCollector.withMutationLock {
                databaseTransactions.write("entry_icon_reclaim") {
                    withContext(Dispatchers.IO) { EntryIconReclaimTransaction.reclaim(this@write, imageRoot, path) }
                }
            }
        }
    }

    override fun discardUnreferencedIcon(path: String) = cleanupQueue.request(path)

    suspend fun drainPendingIcons() = cleanupQueue.drain()
    private val imageRoot: File
        get() = VaultResourcePaths.vaultImagesDir(context)

    suspend fun clean(resources: Collection<DeletedEntryResources>) {
        logger.inSpan(Operations.ResourceCleanup) {
            attachmentGarbageCollector.drain()
            resources.mapNotNull(DeletedEntryResources::customIconPath).forEach(cleanupQueue::enqueue)
            resources.flatMap(DeletedEntryResources::revisionIconPaths).forEach(cleanupQueue::enqueue)
            cleanupQueue.drain()
        }
    }

    suspend fun cleanReplacedIcon(oldPath: String?, newPath: String?) {
        oldPath?.takeIf { it.isNotBlank() && it != newPath }?.let(cleanupQueue::enqueue)
        cleanupQueue.drain()
    }
}
