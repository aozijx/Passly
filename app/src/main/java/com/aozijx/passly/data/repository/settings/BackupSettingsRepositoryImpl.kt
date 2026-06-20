package com.aozijx.passly.data.repository.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.aozijx.passly.data.repository.settings.internal.BACKUP_DIRECTORY_URI_KEY
import com.aozijx.passly.data.repository.settings.internal.LAST_BACKUP_EXPORT_FILE_NAME_KEY
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import com.aozijx.passly.domain.repository.settings.BackupSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettingsRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    BackupSettingsRepository {
    private val appContext = context.applicationContext

    override val backupDirectoryUri: Flow<String?> =
        appContext.settingsDataStore.data.map { it[BACKUP_DIRECTORY_URI_KEY] }
    override val lastBackupExportFileName: Flow<String?> =
        appContext.settingsDataStore.data.map { it[LAST_BACKUP_EXPORT_FILE_NAME_KEY] }

    override suspend fun setBackupDirectoryUri(uri: String) {
        appContext.settingsDataStore.edit { it[BACKUP_DIRECTORY_URI_KEY] = uri }
    }

    override suspend fun clearBackupDirectoryUri() {
        appContext.settingsDataStore.edit { it.remove(BACKUP_DIRECTORY_URI_KEY) }
    }

    override suspend fun setLastBackupExportFileName(fileName: String) {
        appContext.settingsDataStore.edit { it[LAST_BACKUP_EXPORT_FILE_NAME_KEY] = fileName }
    }
}