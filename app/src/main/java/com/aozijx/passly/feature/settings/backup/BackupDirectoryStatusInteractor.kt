package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BackupDirectoryStatusInteractor @Inject constructor(
    settingsSource: BackupDirectorySettingsSource,
    private val statusReader: BackupDirectoryStatusReader,
) {
    val directoryUri: Flow<String?> = settingsSource.backupDirectoryUri

    fun inspect(treeUri: String?): PersistedDirectoryAccessStatus =
        statusReader.inspect(treeUri)
}
