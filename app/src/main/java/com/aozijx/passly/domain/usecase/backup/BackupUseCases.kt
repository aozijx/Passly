package com.aozijx.passly.domain.usecase.backup

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.backup.ImportMode
import com.aozijx.passly.domain.service.backup.VaultBackupService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupUseCases @Inject constructor(private val service: VaultBackupService) {

    suspend fun exportBackup(
        uri: String, password: CharArray, includeImages: Boolean
    ): AppResult<Unit> = service.exportEncryptedBackup(uri, password, includeImages)

    suspend fun importBackup(
        uri: String, password: CharArray, mode: ImportMode
    ): AppResult<Unit> = service.importBackup(uri, password, mode)

    suspend fun exportPlainBackup(uri: String): AppResult<Unit> =
        service.exportPlainBackup(uri)

    suspend fun importPlainBackup(uri: String, mode: ImportMode): AppResult<Unit> =
        service.importPlainBackup(uri, mode)

    suspend fun exportEmergencyBackup(): AppResult<File> =
        service.exportEmergencyBackup()

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit> =
        service.checkDirectoryWritable(uri)
}