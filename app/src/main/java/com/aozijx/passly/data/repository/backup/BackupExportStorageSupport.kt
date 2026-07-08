package com.aozijx.passly.data.repository.backup

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

@Singleton
class BackupExportStorageSupport @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val BACKUP_FILE_MIME = "application/octet-stream"
    private val DIRECTORY_MIME = DocumentsContract.Document.MIME_TYPE_DIR

    data class ExportTarget(
        val fileUri: Uri, val fileName: String, val directoryTreeUri: Uri
    )

    fun buildBackupFileName(
        timestamp: Long = System.currentTimeMillis(), isFull: Boolean = false
    ): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val suffix = if (isFull) "_full" else ""
        return "backup_${formatter.format(Date(timestamp))}$suffix.passly"
    }

    fun defaultDocumentsTreeUri(): Uri = Companion.defaultDocumentsTreeUri()

    fun ensureAppDirectoryTreeUri(selectedTreeUri: Uri): Result<Uri> =
        ensureAppDirectoryTreeUri(context, selectedTreeUri)

    fun createNamedExportTarget(directoryTreeUri: String, fileName: String): Result<ExportTarget> =
        runCatching {
            val treeUri = directoryTreeUri.toUri()
            val resolver = context.contentResolver

            if (!hasTreeUriPermission(treeUri)) {
                error("目录访问权限已失效，请重新选择备份目录")
            }

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val fileUri = try {
                DocumentsContract.createDocument(resolver, parentDocUri, BACKUP_FILE_MIME, fileName)
                    ?: error("无法创建备份文件")
            } catch (e: SecurityException) {
                error("目录访问权限已失效，请重新选择备份目录")
            }
            ExportTarget(fileUri = fileUri, fileName = fileName, directoryTreeUri = treeUri)
        }

    fun deleteDocument(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) {
            false
        }
    }

    fun testWritePermission(directoryTreeUri: String): Result<String> =
        runCatching {
            val treeUri = directoryTreeUri.toUri()
            val resolver = context.contentResolver
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val probeName = "permission_probe_${System.currentTimeMillis()}.tmp"
            val probeUri = try {
                DocumentsContract.createDocument(
                    resolver, parentDocUri, BACKUP_FILE_MIME, probeName
                ) ?: error("无法创建测试文件")
            } catch (e: SecurityException) {
                error("目录访问权限已失效，请重新选择备份目录")
            }
            runCatching { DocumentsContract.deleteDocument(resolver, probeUri) }
            probeName
        }

    private fun findChildDirectoryByName(treeUri: Uri, parentDocId: String, name: String): Uri? {
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

    private fun queryDisplayName(documentUri: Uri): String? {
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

    fun hasTreeUriPermission(treeUri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == treeUri && permission.isWritePermission
        }
    }

    companion object {
        private const val BACKUP_FILE_MIME_STATIC = "application/octet-stream"
        private const val DIRECTORY_MIME_STATIC = DocumentsContract.Document.MIME_TYPE_DIR

        fun buildBackupFileName(
            timestamp: Long = System.currentTimeMillis(), isFull: Boolean = false
        ): String {
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val suffix = if (isFull) "_full" else ""
            return "backup_${formatter.format(Date(timestamp))}$suffix.passly"
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
                        ?: try {
                            DocumentsContract.createDocument(
                                resolver, rootDocUri, DIRECTORY_MIME_STATIC, appName
                            ) ?: error("无法创建应用目录")
                        } catch (e: SecurityException) {
                            error("目录访问权限不足，请重新选择备份目录")
                        }
                }

                val authority = selectedTreeUri.authority ?: error("目录授权无效")
                val appDocId = DocumentsContract.getDocumentId(appDocUri)
                DocumentsContract.buildTreeDocumentUri(authority, appDocId)
                    ?: error("无法构建应用目录树 URI")
            }

        fun createNamedExportTarget(
            context: Context, directoryTreeUri: String, fileName: String
        ): Result<ExportTarget> = runCatching {
            val treeUri = directoryTreeUri.toUri()
            val resolver = context.contentResolver

            if (!hasTreeUriPermission(context, treeUri)) {
                error("目录访问权限已失效，请重新选择备份目录")
            }

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val fileUri = try {
                DocumentsContract.createDocument(resolver, parentDocUri, BACKUP_FILE_MIME_STATIC, fileName)
                    ?: error("无法创建备份文件")
            } catch (e: SecurityException) {
                error("目录访问权限已失效，请重新选择备份目录")
            }
            ExportTarget(fileUri = fileUri, fileName = fileName, directoryTreeUri = treeUri)
        }

        fun deleteDocument(context: Context, uri: Uri): Boolean {
            return try {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (e: Exception) {
                false
            }
        }

        fun testWritePermission(context: Context, directoryTreeUri: String): Result<String> =
            runCatching {
                val treeUri = directoryTreeUri.toUri()
                val resolver = context.contentResolver
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val probeName = "permission_probe_${System.currentTimeMillis()}.tmp"
                val probeUri = try {
                    DocumentsContract.createDocument(
                        resolver, parentDocUri, BACKUP_FILE_MIME_STATIC, probeName
                    ) ?: error("无法创建测试文件")
                } catch (e: SecurityException) {
                    error("目录访问权限已失效，请重新选择备份目录")
                }
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
                    if (displayName == name && mimeType == DIRECTORY_MIME_STATIC) {
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

        fun hasTreeUriPermission(context: Context, treeUri: Uri): Boolean {
            return context.contentResolver.persistedUriPermissions.any { permission ->
                permission.uri == treeUri && permission.isWritePermission
            }
        }
    }
}