package com.aozijx.passly.domain.settings.port

import kotlinx.coroutines.flow.Flow

interface BackupDirectorySettingsSource {
    val backupDirectoryUri: Flow<String?>
}

interface BackupDirectorySettingsRepository : BackupDirectorySettingsSource {
    suspend fun setBackupDirectoryUri(uri: String)
    suspend fun clearBackupDirectoryUri()
}
