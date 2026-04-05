package com.aozijx.passly.core.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.aozijx.passly.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExportStorageSupport {

    private const val BACKUP_FILE_MIME = "application/octet-stream"
    private const val DIRECTORY_MIME = DocumentsContract.Document.MIME_TYPE_DIR

    data class ExportTarget(
        val fileUri: Uri, val fileName: String, val directoryTreeUri: Uri
    )

    fun buildBackupFileName(timestamp: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "backup_${formatter.format(Date(timestamp))}.passly"
    }

    fun defaultDocumentsTreeUri(): Uri = DocumentsContract.buildTreeDocumentUri(
        "com.android.externalstorage.documents", "primary:Documents"
    )

    fun ensureAppDirectoryTreeUri(context: Context, selectedTreeUri: Uri): Result<Uri> =
        runCatching {
            val resolver = context.contentResolver
            val appName = context.getString(R.string.app_name)
            val treeDocId = DocumentsContract.getTreeDocumentId(selectedTreeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(selectedTreeUri, treeDocId)

            val rootName = queryDisplayName(context, rootDocUri)
            val appDocUri = if (rootName == appName) {
                rootDocUri
            } else {
                findChildDirectoryByName(context, selectedTreeUri, treeDocId, appName)
                    ?: DocumentsContract.createDocument(
                        resolver, rootDocUri, DIRECTORY_MIME, appName
                    ) ?: error("无法创建应用目录")
            }

            val authority = selectedTreeUri.authority ?: error("目录授权无效")
            val appDocId = DocumentsContract.getDocumentId(appDocUri)
            DocumentsContract.buildTreeDocumentUri(authority, appDocId)
                ?: error("无法构建应用目录树 URI")
        }

    fun createTimestampExportTarget(
        context: Context, directoryTreeUri: String
    ): Result<ExportTarget> = runCatching {
        val treeUri = directoryTreeUri.toUri()
        val resolver = context.contentResolver
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val fileName = buildBackupFileName()
        val fileUri =
            DocumentsContract.createDocument(resolver, parentDocUri, BACKUP_FILE_MIME, fileName)
                ?: error("无法创建备份文件")
        ExportTarget(fileUri = fileUri, fileName = fileName, directoryTreeUri = treeUri)
    }

    fun testWritePermission(context: Context, directoryTreeUri: String): Result<String> =
        runCatching {
            val treeUri = directoryTreeUri.toUri()
            val resolver = context.contentResolver
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val probeName = "permission_probe_${System.currentTimeMillis()}.tmp"
            val probeUri = DocumentsContract.createDocument(
                resolver, parentDocUri, BACKUP_FILE_MIME, probeName
            ) ?: error("无法创建测试文件")
            runCatching { DocumentsContract.deleteDocument(resolver, probeUri) }
            probeName
        }

    private fun findChildDirectoryByName(
        context: Context, treeUri: Uri, parentDocId: String, name: String
    ): Uri? {
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        resolver.query(
            childrenUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeIndex)
                if (displayName == name && mimeType == DIRECTORY_MIME) {
                    val childId = cursor.getString(idIndex)
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                }
            }
        }
        return null
    }

    private fun queryDisplayName(context: Context, documentUri: Uri): String? {
        val resolver = context.contentResolver
        resolver.query(
            documentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }
}

