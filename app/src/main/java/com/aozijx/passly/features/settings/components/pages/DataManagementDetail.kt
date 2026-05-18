package com.aozijx.passly.features.settings.components.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.SettingsContentActions
import com.aozijx.passly.features.settings.SettingsContentState
import com.aozijx.passly.features.settings.components.sections.BackupRestoreSettingsSection
import com.aozijx.passly.features.settings.components.sections.DataSettingsSection

@Composable
internal fun DataManagementDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        DataSettingsSection(
            isAutoDownloadIcons = state.isAutoDownloadIcons,
            onAutoDownloadIconsChange = actions.onAutoDownloadIconsChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        BackupRestoreSettingsSection(
            pathLabel = state.backupPathLabel,
            recentExportFileName = state.lastExportFileLabel,
            onPickPath = actions.onPickBackupPath,
            onTestWrite = actions.onTestBackupWrite,
            onClearPath = actions.onClearBackupPath
        )
    }
}