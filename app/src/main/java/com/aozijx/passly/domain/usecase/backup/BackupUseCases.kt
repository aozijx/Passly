package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.domain.model.backup.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository

class BackupUseCases(private val repository: BackupRepository) {

    suspend fun exportBackup(
        uri: Uri, password: CharArray, includeImages: Boolean
    ): Result<Unit> = repository.exportEncryptedBackup(uri, password, includeImages)

    suspend fun importBackup(
        uri: Uri, password: CharArray, mode: BackupImportMode
    ): Result<Unit> = repository.importBackup(uri, password, mode)

    suspend fun exportPlainBackup(uri: Uri) = repository.exportPlainBackup(uri)

    suspend fun exportEmergencyBackup() = repository.exportEmergencyBackup()

    suspend fun testDirectoryWritePermission(directoryUri: String) =
        repository.testDirectoryWritePermission(directoryUri)
}