package com.aozijx.passly.features.settings.components.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.components.common.ClickableSettingItem

@Composable
fun BackupPathSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    ClickableSettingItem(
        icon = Icons.Default.FolderOpen,
        title = "备份目录",
        longValue = pathLabel,
        onClick = onPickPath
    )

    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
    ClickableSettingItem(
        icon = Icons.Default.TaskAlt, title = "测试写入权限", value = null, onClick = onTestWrite
    )

    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
    ClickableSettingItem(
        icon = Icons.Default.FolderOpen,
        title = "最近导出文件",
        longValue = recentExportFileName,
        onClick = {})

    if (onClearPath != null) {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            icon = Icons.Default.DeleteOutline,
            title = "清除备份目录",
            value = null,
            onClick = onClearPath
        )
    }
}
