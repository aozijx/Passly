package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun BackupRestoreSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    SettingsSectionTitle(text = "备份与恢复")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "backup.directory",
                title = "备份目录",
                subtitle = pathLabel,
                onClick = onPickPath
            ),
            navigationSettingsGroupItem(
                key = "backup.test_write",
                title = "测试写入权限",
                onClick = onTestWrite
            ),
            navigationSettingsGroupItem(
                key = "backup.last_export",
                title = "最近导出文件",
                subtitle = recentExportFileName,
                onClick = {}
            ),
            navigationSettingsGroupItem(
                key = "backup.clear_directory",
                visible = onClearPath != null,
                title = "清除备份目录",
                onClick = onClearPath ?: {}
            )
        )
    )
}
