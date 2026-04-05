package com.aozijx.passly.features.settings.components.sections

import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle

@Composable
fun BackupRestoreSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    SettingsGroupTitle(text = "备份与恢复")
    SettingsCard {
        BackupPathSettingsSection(
            pathLabel = pathLabel,
            recentExportFileName = recentExportFileName,
            onPickPath = onPickPath,
            onTestWrite = onTestWrite,
            onClearPath = onClearPath
        )
    }
}
