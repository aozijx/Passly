package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
internal fun BackupRestoreSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    SettingsSectionTitle(text = "备份与恢复")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "backup.export",
                icon = Icons.Default.FileUpload,
                title = "导出",
                subtitle = "加密备份、JSON 或可读文本",
                onClick = onExport
            ),
            navigationSettingsGroupItem(
                key = "backup.import",
                icon = Icons.Default.FileDownload,
                title = "导入",
                subtitle = "自动识别 Passly 与兼容格式",
                onClick = onImport
            )
        )
    )

    SettingsSectionTitle(text = "存储位置")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "backup.directory",
                title = "默认备份目录",
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
                title = "清除默认目录",
                onClick = onClearPath ?: {}
            )
        )
    )
}
