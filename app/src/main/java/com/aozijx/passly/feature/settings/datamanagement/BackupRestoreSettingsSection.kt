package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.components.navigationSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
fun BackupRestoreSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    SettingsGroupTitle(text = "备份与恢复")
    SettingsRoundedGroup {
        navigationSettingsItem(
            title = "备份目录",
            subtitle = pathLabel,
            onClick = onPickPath
        )
        navigationSettingsItem(
            title = "测试写入权限",
            onClick = onTestWrite
        )
        navigationSettingsItem(
            title = "最近导出文件",
            subtitle = recentExportFileName,
            onClick = {}
        )
        navigationSettingsItem(
            visible = onClearPath != null,
            title = "清除备份目录",
            onClick = onClearPath ?: {}
        )
    }
}