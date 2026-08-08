package com.aozijx.passly.data.repository.entry.internal

import android.content.Context
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DeletedEntryResources(
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
class EntryResourceCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val attachmentRoot: File
        get() = VaultResourcePaths.attachmentDir(context)

    private val imageRoot: File
        get() = VaultResourcePaths.vaultImagesDir(context)

    suspend fun clean(resources: Collection<DeletedEntryResources>) = withContext(Dispatchers.IO) {
        resources
            .distinctBy(DeletedEntryResources::entryId)
            .forEach { resource ->
                runCatching {
                    deleteEntryAttachments(resource.entryId)
                    deleteCustomIcon(resource.customIconPath)
                }.onFailure { error ->
                    AppTelemetry.w(
                        EventCategory.FILE_IO,
                        "trash.resource_cleanup_failed",
                        throwable = error
                    )
                }
            }
    }

    private fun deleteEntryAttachments(entryId: String) {
        val root = attachmentRoot.canonicalFile
        val target = File(root, entryId).canonicalFile
        require(target.parentFile == root) { "Invalid attachment cleanup target" }
        if (target.exists()) {
            require(target.deleteRecursively()) { "Unable to delete attachment directory" }
        }
    }

    private fun deleteCustomIcon(path: String?) {
        val normalizedPath = path
            ?.trim()
            ?.removePrefix(FILE_SCHEME)
            ?.takeIf(String::isNotEmpty)
            ?: return
        val root = imageRoot.canonicalFile
        val target = File(normalizedPath).canonicalFile
        if (target.parentFile != root || !target.exists()) return
        require(target.delete()) { "Unable to delete custom icon" }
    }

    private companion object {
        const val FILE_SCHEME = "file://"
    }
}
