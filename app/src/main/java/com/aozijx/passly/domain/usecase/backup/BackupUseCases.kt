package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository

class BackupUseCases(private val repository: BackupRepository) {

    suspend fun exportBackup(
        uri: Uri, password: CharArray, includeImages: Boolean
    ): AppResult<Unit> = AppResult.runSuspendCatching("domain.backup.exportEncrypted") {
        repository.exportEncryptedBackup(uri, password, includeImages)
    }

    suspend fun importBackup(
        uri: Uri, password: CharArray, mode: BackupImportMode
    ): AppResult<Unit> = AppResult.runSuspendCatching("domain.backup.import") {
        repository.importBackup(uri, password, mode)
    }

    suspend fun exportPlainBackup(uri: Uri): AppResult<Unit> =
        AppResult.runSuspendCatching("domain.backup.exportPlain") {
            repository.exportPlainBackup(uri)
        }

    suspend fun exportEmergencyBackup() =
        AppResult.runSuspendCatching("domain.backup.exportEmergency") {
            repository.exportEmergencyBackup()
        }

    suspend fun testDirectoryWritePermission(directoryUri: String): AppResult<Unit> =
        AppResult.runSuspendCatching("domain.backup.testDirectoryWrite") {
            repository.testDirectoryWritePermission(directoryUri)
        }
}