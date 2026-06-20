package com.aozijx.passly.domain.usecase.settings.backup

import com.aozijx.passly.domain.repository.settings.BackupSettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * 备份级设置用例：负责备份路径、备份文件名等备份相关设置
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettingsUseCases @Inject constructor(private val repository: BackupSettingsRepository) {
    val backupDirectoryUri: Flow<String?> = repository.backupDirectoryUri
    val lastBackupExportFileName: Flow<String?> = repository.lastBackupExportFileName

    suspend fun setBackupDirectoryUri(uri: String) = repository.setBackupDirectoryUri(uri)
    suspend fun clearBackupDirectoryUri() = repository.clearBackupDirectoryUri()
    suspend fun setLastBackupExportFileName(fileName: String) =
        repository.setLastBackupExportFileName(fileName)
}