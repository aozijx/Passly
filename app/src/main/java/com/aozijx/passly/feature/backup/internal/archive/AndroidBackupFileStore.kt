package com.aozijx.passly.feature.backup.internal.archive

import android.content.Context
import androidx.core.net.toUri
import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidBackupFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupFileStore {

    private fun openInputStream(uri: String): InputStream {
        val parsed = uri.toUri()
        return try {
            context.contentResolver.openInputStream(parsed)
                ?: throw BackupFailed()
        } catch (_: SecurityException) {
            throw BackupFailed()
        } catch (_: FileNotFoundException) {
            throw BackupFailed()
        }
    }

    private fun openOutputStream(uri: String): OutputStream {
        val parsed = uri.toUri()
        return try {
            context.contentResolver.openOutputStream(parsed, "rwt")
                ?: throw BackupFailed()
        } catch (_: SecurityException) {
            throw BackupFailed()
        } catch (_: FileNotFoundException) {
            throw BackupFailed()
        }
    }

    override fun writeBytes(uri: String, data: ByteArray) {
        openOutputStream(uri).use { it.write(data); it.flush() }
    }

    override fun readBytesSafely(uri: String, maxBytes: Long): ByteArray {
        val stream = openInputStream(uri)
        return stream.use { input ->
            val limited = com.aozijx.passly.feature.backup.internal.archive.io.LimitedInputStream(input, maxBytes)
            limited.readBytes()
        }
    }

    override suspend fun checkWritable(uri: String): AppResult<Unit> =
        AppResult.runSuspendCatching {
            val parsed = uri.toUri()
            val persistedWritable = context.contentResolver.persistedUriPermissions.any {
                it.uri == parsed && it.isWritePermission
            }
            if (!persistedWritable) {
                context.contentResolver.openFileDescriptor(parsed, "rw")
                    ?.use { /* 打开但不写入，避免截断现有文件。 */ }
                    ?: throw BackupFailed()
            }
        }
}
