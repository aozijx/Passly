package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.settings.shell.sectionSpacing

@Composable
internal fun DataManagementDetail(
    state: DataUiState,
    backupPathLabel: String,
    lastExportFileLabel: String,
    onAutoDownloadIconsChange: (Boolean) -> Unit,
    onFaviconWhitelistChange: (String) -> Unit,
    onPickBackupPath: () -> Unit,
    onTestBackupWrite: () -> Unit,
    onClearBackupPath: (() -> Unit)?
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        DataSettingsSection(
            isAutoDownloadIcons = state.isAutoDownloadIcons,
            faviconDownloadWhitelist = state.faviconDownloadWhitelist,
            onAutoDownloadIconsChange = onAutoDownloadIconsChange,
            onFaviconWhitelistChange = onFaviconWhitelistChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        BackupRestoreSettingsSection(
            pathLabel = backupPathLabel,
            recentExportFileName = lastExportFileLabel,
            onPickPath = onPickBackupPath,
            onTestWrite = onTestBackupWrite,
            onClearPath = onClearBackupPath
        )
    }
}