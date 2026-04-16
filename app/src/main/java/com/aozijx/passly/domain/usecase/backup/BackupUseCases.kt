package com.aozijx.passly.domain.usecase.backup

import com.aozijx.passly.domain.repository.backup.BackupRepository

/**
 * 备份/恢复业务用例门面类。
 */
class BackupUseCases(private val repository: BackupRepository) {
    val exportBackup = ExportBackupUseCase(repository)
    val importBackup = ImportBackupUseCase(repository)

    suspend fun exportPlainBackup(uri: android.net.Uri) = repository.exportPlainBackup(uri)
    suspend fun exportEmergencyBackup() = repository.exportEmergencyBackup()

    suspend fun testDirectoryWritePermission(directoryUri: String) =
        repository.testDirectoryWritePermission(directoryUri)
}