package com.aozijx.passly.domain.repository.settings

import kotlinx.coroutines.flow.Flow

interface BackupSettingsRepository {
    val backupDirectoryUri: Flow<String?>
    val lastBackupExportFileName: Flow<String?>

    suspend fun setBackupDirectoryUri(uri: String)
    suspend fun clearBackupDirectoryUri()
    suspend fun setLastBackupExportFileName(fileName: String)
}