package com.aozijx.passly.data.repository.backup.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.domain.model.core.BackupException
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

internal fun Context.openBackupInputStream(uri: Uri): InputStream {
    return try {
        contentResolver.openInputStream(uri) ?: throw BackupException.FileCorrupted()
    } catch (_: SecurityException) {
        throw BackupException.StoragePermissionDenied()
    } catch (_: FileNotFoundException) {
        throw BackupException.FileCorrupted()
    }
}

internal fun Context.openBackupOutputStream(uri: Uri): OutputStream {
    return try {
        contentResolver.openOutputStream(uri)
            ?: throw BackupException.StoragePermissionDenied()
    } catch (_: SecurityException) {
        throw BackupException.StoragePermissionDenied()
    } catch (_: FileNotFoundException) {
        throw BackupException.StoragePermissionDenied()
    }
}