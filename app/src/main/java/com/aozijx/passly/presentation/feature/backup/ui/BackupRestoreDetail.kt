package com.aozijx.passly.presentation.feature.backup.ui

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection

@Composable
internal fun BackupRestoreDetail(
    backupPathLabel: String,
    lastExportFileLabel: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onPickBackupPath: () -> Unit,
    onTestBackupWrite: () -> Unit,
    onClearBackupPath: (() -> Unit)?
) {
    SettingsSection {
        BackupRestoreSettingsSection(
            pathLabel = backupPathLabel,
            recentExportFileName = lastExportFileLabel,
            onExport = onExport,
            onImport = onImport,
            onPickPath = onPickBackupPath,
            onTestWrite = onTestBackupWrite,
            onClearPath = onClearBackupPath
        )
    }
}
