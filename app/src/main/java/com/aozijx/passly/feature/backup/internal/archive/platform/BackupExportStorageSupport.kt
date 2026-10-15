package com.aozijx.passly.feature.backup.internal.archive.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.aozijx.passly.app.backup.BackupDirectoryGrantInspector
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.feature.backup.internal.archive.di.BackupIoDispatcher
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport.ExportTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * UI storage helper for creating backup destinations through SAF.
 *
 * Backup encoding and restoration deliberately live outside this class.
 */
@Singleton
internal class BackupExportStorageSupport @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val grantInspector: BackupDirectoryGrantInspector,
    @param:BackupIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupStorageSupport {

    override fun buildBackupFileName(
        extension: String,
        timestamp: Long,
    ): String {
        require(extension.matches(Regex("[a-z0-9]{2,12}")))
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "backup_${formatter.format(Date(timestamp))}.$extension"
    }

    override suspend fun createNamedExportTarget(
        directoryTreeUri: String,
        fileName: String,
        mimeType: String,
    ): Result<ExportTarget> = withContext(ioDispatcher) {
        runCatching {
            val grantedTreeUri = directoryTreeUri.toUri()
            val resolver = context.contentResolver
            require(
                grantInspector.inspect(directoryTreeUri) == PersistedDirectoryAccessStatus.GRANTED,
            ) {
                "目录访问权限已失效，请重新选择备份目录"
            }
            val treeUri = BackupStorageSupport.ensureAppDirectoryTreeUri(
                context,
                grantedTreeUri,
            ).getOrThrow()
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val fileUri = try {
                DocumentsContract.createDocument(
                    resolver,
                    parentDocUri,
                    mimeType,
                    fileName,
                ) ?: error("无法创建备份文件")
            } catch (error: SecurityException) {
                error("目录访问权限已失效，请重新选择备份目录")
            }
            ExportTarget(fileUri, fileName, treeUri)
        }
    }

    override suspend fun deleteDocument(uri: Uri): Boolean = withContext(ioDispatcher) {
        runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        }.getOrDefault(false)
    }

    override suspend fun checkDirectoryWritable(
        directoryTreeUri: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            require(
                grantInspector.inspect(directoryTreeUri) == PersistedDirectoryAccessStatus.GRANTED,
            ) { "目录访问权限已失效，请重新选择备份目录" }
            val resolver = context.contentResolver
            val treeUri = BackupStorageSupport.ensureAppDirectoryTreeUri(
                context,
                directoryTreeUri.toUri(),
            ).getOrThrow()
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            val testFile = DocumentsContract.createDocument(
                resolver,
                parentUri,
                "application/octet-stream",
                WRITE_TEST_FILE_NAME,
            ) ?: error("无法在备份目录创建测试文件")
            var primaryFailure: Throwable? = null
            try {
                resolver.openOutputStream(testFile, "rwt")?.use { output ->
                    output.write(WRITE_TEST_BYTE)
                    output.flush()
                } ?: error("无法写入备份目录测试文件")
            } catch (failure: Throwable) {
                primaryFailure = failure
                throw failure
            } finally {
                enforceWriteTestCleanup(
                    primaryFailure = primaryFailure,
                    cleanupResult = runCatching {
                        DocumentsContract.deleteDocument(resolver, testFile)
                    },
                )
            }
        }
    }

    private companion object {
        const val WRITE_TEST_FILE_NAME = ".passly_write_test.tmp"
        const val WRITE_TEST_BYTE = 0
    }
}

internal fun enforceWriteTestCleanup(
    primaryFailure: Throwable?,
    cleanupResult: Result<Boolean>,
) {
    val cleanupFailure = cleanupResult.fold(
        onSuccess = { deleted ->
            if (deleted) null else IllegalStateException("无法清理备份目录测试文件")
        },
        onFailure = { it },
    ) ?: return
    if (primaryFailure != null) {
        primaryFailure.addSuppressed(cleanupFailure)
    } else {
        throw cleanupFailure
    }
}
