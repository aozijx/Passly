package com.aozijx.passly.data.backup

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.BackupFailed
import com.aozijx.passly.data.backup.io.BackupFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBackupFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupFileStore {

    private fun openInputStream(uri: String): InputStream {
        val parsed = Uri.parse(uri)
        return try {
            context.contentResolver.openInputStream(parsed)
                ?: throw BackupFailed("无法打开备份输入流")
        } catch (_: SecurityException) {
            throw BackupFailed("没有文件读取权限，请重新授权")
        } catch (_: FileNotFoundException) {
            throw BackupFailed("备份文件未找到")
        }
    }

    private fun openOutputStream(uri: String): OutputStream {
        val parsed = Uri.parse(uri)
        return try {
            context.contentResolver.openOutputStream(parsed, "rwt")
                ?: throw BackupFailed("没有文件写入权限，请重新授权")
        } catch (_: SecurityException) {
            throw BackupFailed("没有文件写入权限，请重新授权")
        } catch (_: FileNotFoundException) {
            throw BackupFailed("无法创建备份文件")
        }
    }

    override fun writeBytes(uri: String, data: ByteArray) {
        openOutputStream(uri).use { it.write(data); it.flush() }
    }

    override fun readBytesSafely(uri: String, maxBytes: Long): ByteArray {
        val stream = openInputStream(uri)
        return stream.use { input ->
            val limited = com.aozijx.passly.data.backup.io.LimitedInputStream(input, maxBytes)
            limited.readBytes()
        }
    }

    override suspend fun checkWritable(uri: String): AppResult<Unit> =
        AppResult.runSuspendCatching("backup.checkWritable") {
            val parsed = Uri.parse(uri)
            val persistedWritable = context.contentResolver.persistedUriPermissions.any {
                it.uri == parsed && it.isWritePermission
            }
            if (!persistedWritable) {
                context.contentResolver.openFileDescriptor(parsed, "rw")
                    ?.use { /* 打开但不写入，避免截断现有文件。 */ }
                    ?: throw BackupFailed("没有文件写入权限，请重新授权")
            }
        }
}
