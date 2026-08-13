package com.aozijx.passly.core.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

interface BackupStorageSupport {
    data class ExportTarget(
        val fileUri: Uri,
        val fileName: String,
        val directoryTreeUri: Uri
    )

    fun buildBackupFileName(
        extension: String = "passly",
        timestamp: Long = System.currentTimeMillis()
    ): String

    fun createNamedExportTarget(
        directoryTreeUri: String,
        fileName: String,
        mimeType: String = BACKUP_FILE_MIME
    ): Result<ExportTarget>

    fun deleteDocument(uri: Uri): Boolean

    companion object {
        internal const val BACKUP_FILE_MIME = "application/octet-stream"
        internal const val DIRECTORY_MIME = DocumentsContract.Document.MIME_TYPE_DIR

        fun defaultDocumentsTreeUri(): Uri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Documents"
        )

        fun ensureAppDirectoryTreeUri(
            context: Context,
            selectedTreeUri: Uri
        ): Result<Uri> = runCatching {
            val resolver = context.contentResolver
            val appName = context.applicationInfo
                .loadLabel(context.packageManager)
                .toString()
            val treeDocId = DocumentsContract.getTreeDocumentId(selectedTreeUri)
            val rootDocUri =
                DocumentsContract.buildDocumentUriUsingTree(selectedTreeUri, treeDocId)
            val appDocUri = if (queryDisplayName(context, rootDocUri) == appName) {
                rootDocUri
            } else {
                findChildDirectoryByName(context, selectedTreeUri, treeDocId, appName)
                    ?: try {
                        DocumentsContract.createDocument(
                            resolver,
                            rootDocUri,
                            DIRECTORY_MIME,
                            appName
                        ) ?: error("无法创建应用目录")
                    } catch (error: SecurityException) {
                        error("目录访问权限不足，请重新选择备份目录")
                    }
            }
            val authority = selectedTreeUri.authority ?: error("目录授权无效")
            DocumentsContract.buildTreeDocumentUri(
                authority,
                DocumentsContract.getDocumentId(appDocUri)
            )
        }
    }
}

private fun findChildDirectoryByName(
    context: Context,
    treeUri: Uri,
    parentDocId: String,
    name: String
): Uri? {
    val childrenUri =
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        null,
        null,
        null
    )?.use { cursor ->
        val idIndex =
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex =
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex =
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == name &&
                cursor.getString(mimeIndex) == BackupStorageSupport.DIRECTORY_MIME
            ) {
                return DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    cursor.getString(idIndex)
                )
            }
        }
    }
    return null
}

private fun queryDisplayName(context: Context, documentUri: Uri): String? {
    context.contentResolver.query(
        documentUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return null
}
