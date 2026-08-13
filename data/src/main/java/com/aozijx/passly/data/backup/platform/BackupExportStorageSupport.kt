package com.aozijx.passly.data.backup.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.aozijx.passly.core.backup.BackupStorageSupport
import com.aozijx.passly.core.backup.BackupStorageSupport.ExportTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI storage helper for creating backup destinations through SAF.
 *
 * Backup encoding and restoration deliberately live outside this class.
 */
@Singleton
internal class BackupExportStorageSupport @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupStorageSupport {

    override fun buildBackupFileName(
        extension: String,
        timestamp: Long
    ): String {
        require(extension.matches(Regex("[a-z0-9]{2,12}")))
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "backup_${formatter.format(Date(timestamp))}.$extension"
    }

    override fun createNamedExportTarget(
        directoryTreeUri: String,
        fileName: String,
        mimeType: String
    ): Result<ExportTarget> = runCatching {
        val grantedTreeUri = directoryTreeUri.toUri()
        val resolver = context.contentResolver
        require(hasTreeUriPermission(context, grantedTreeUri)) {
            "目录访问权限已失效，请重新选择备份目录"
        }
        val treeUri =
            BackupStorageSupport.ensureAppDirectoryTreeUri(context, grantedTreeUri).getOrThrow()
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val fileUri = try {
            DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                mimeType,
                fileName
            ) ?: error("无法创建备份文件")
        } catch (error: SecurityException) {
            error("目录访问权限已失效，请重新选择备份目录")
        }
        ExportTarget(fileUri, fileName, treeUri)
    }

    override fun deleteDocument(uri: Uri): Boolean = runCatching {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    }.getOrDefault(false)

    companion object {
        private fun hasTreeUriPermission(context: Context, treeUri: Uri): Boolean =
            context.contentResolver.persistedUriPermissions.any {
                it.uri == treeUri && it.isWritePermission
            }
    }
}
