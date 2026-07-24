package com.aozijx.passly.feature.backup.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.aozijx.passly.R
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
class BackupExportStorageSupport @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class ExportTarget(
        val fileUri: Uri,
        val fileName: String,
        val directoryTreeUri: Uri
    )

    fun buildBackupFileName(timestamp: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "backup_${formatter.format(Date(timestamp))}.passly"
    }

    fun createNamedExportTarget(
        directoryTreeUri: String,
        fileName: String
    ): Result<ExportTarget> = runCatching {
        val treeUri = directoryTreeUri.toUri()
        val resolver = context.contentResolver
        require(hasTreeUriPermission(context, treeUri)) {
            "目录访问权限已失效，请重新选择备份目录"
        }
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val fileUri = try {
            DocumentsContract.createDocument(
                resolver,
                parentDocUri,
                BACKUP_FILE_MIME,
                fileName
            ) ?: error("无法创建备份文件")
        } catch (error: SecurityException) {
            error("目录访问权限已失效，请重新选择备份目录")
        }
        ExportTarget(fileUri, fileName, treeUri)
    }

    fun deleteDocument(uri: Uri): Boolean = runCatching {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    }.getOrDefault(false)

    companion object {
        private const val BACKUP_FILE_MIME = "application/octet-stream"
        private const val DIRECTORY_MIME = DocumentsContract.Document.MIME_TYPE_DIR

        fun defaultDocumentsTreeUri(): Uri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Documents"
        )

        fun ensureAppDirectoryTreeUri(
            context: Context,
            selectedTreeUri: Uri
        ): Result<Uri> = runCatching {
            val resolver = context.contentResolver
            val appName = context.getString(R.string.app_name)
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
                        cursor.getString(mimeIndex) == DIRECTORY_MIME
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

        private fun hasTreeUriPermission(context: Context, treeUri: Uri): Boolean =
            context.contentResolver.persistedUriPermissions.any {
                it.uri == treeUri && it.isWritePermission
            }
    }
}
