package com.aozijx.passly.data.backup

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.backup.BackupException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBackupFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun openInputStream(uri: String): InputStream {
        val parsed = Uri.parse(uri)
        return try {
            context.contentResolver.openInputStream(parsed)
                ?: throw BackupException.FileCorrupted()
        } catch (_: SecurityException) {
            throw BackupException.StoragePermissionDenied()
        } catch (_: FileNotFoundException) {
            throw BackupException.FileCorrupted()
        }
    }

    fun openOutputStream(uri: String): OutputStream {
        val parsed = Uri.parse(uri)
        return try {
            context.contentResolver.openOutputStream(parsed)
                ?: throw BackupException.StoragePermissionDenied()
        } catch (_: SecurityException) {
            throw BackupException.StoragePermissionDenied()
        } catch (_: FileNotFoundException) {
            throw BackupException.StoragePermissionDenied()
        }
    }

    fun readBytes(uri: String): ByteArray = openInputStream(uri).use { it.readBytes() }

    fun writeBytes(uri: String, data: ByteArray) {
        openOutputStream(uri).use { it.write(data); it.flush() }
    }

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit> =
        AppResult.runSuspendCatching("backup.checkWritable") {
            writeBytes(uri, ByteArray(0))
        }

    fun imageDirectory(): File =
        File(context.filesDir, "vault_images").apply { mkdirs() }

    fun imageEntryName(entryId: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(entryId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "images/${digest.take(32)}.bin"
    }
}
