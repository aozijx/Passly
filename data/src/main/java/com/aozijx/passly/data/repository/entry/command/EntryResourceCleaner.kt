package com.aozijx.passly.data.repository.entry.command

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

internal data class DeletedEntryResources(
    val entryId: String,
    val customIconPath: String?
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
    private val telemetry: TelemetryReporter,
) {
    private val imageRoot: File
        get() = VaultResourcePaths.vaultImagesDir(context)

    suspend fun clean(resources: Collection<DeletedEntryResources>) {
        attachmentGarbageCollector.drain()
        withContext(Dispatchers.IO) {
            resources
                .distinctBy(DeletedEntryResources::entryId)
                .forEach { resource ->
                    runCatching {
                        deleteCustomIcon(resource.customIconPath)
                    }.onFailure { error ->
                        telemetry.report(
                            EventLevel.WARN,
                            EventCategory.FILE_IO,
                            "trash.resource_cleanup_failed",
                            error
                        )
                    }
                }
        }
    }

    suspend fun cleanReplacedIcon(oldPath: String?, newPath: String?) {
        val candidate = oldPath?.takeIf { it.isNotBlank() && it != newPath } ?: return
        val references = databaseTransactions.read("entry_icon_reference_check") {
            entryQueryDao().countByIconCustomReference(candidate)
        }.getOrNull() ?: return
        if (references != 0) return
        withContext(Dispatchers.IO) {
            runCatching { deleteCustomIcon(candidate) }.onFailure { error ->
                telemetry.report(
                    EventLevel.WARN,
                    EventCategory.FILE_IO,
                    "entry.icon_replacement_cleanup_failed",
                    error,
                )
            }
        }
    }

    private fun deleteCustomIcon(path: String?) {
        val normalizedPath = path?.trim()?.takeIf(String::isNotEmpty) ?: return
        val root = imageRoot.canonicalFile
        val target = File(normalizedPath).canonicalFile
        if (target.parentFile != root || !target.exists()) return
        require(target.delete()) { "Unable to delete custom icon" }
    }
}
