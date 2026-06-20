package com.aozijx.passly.ui.features.settings.data

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun DataManagementDetail(
    state: DataUiState,
    backupPathLabel: String,
    lastExportFileLabel: String,
    onAutoDownloadIconsChange: (Boolean) -> Unit,
    onPickBackupPath: () -> Unit,
    onTestBackupWrite: () -> Unit,
    onClearBackupPath: (() -> Unit)?
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        DataSettingsSection(
            isAutoDownloadIcons = state.isAutoDownloadIcons,
            onAutoDownloadIconsChange = onAutoDownloadIconsChange
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