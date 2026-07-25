package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        Spacer(modifier = Modifier.height(8.dp))
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
