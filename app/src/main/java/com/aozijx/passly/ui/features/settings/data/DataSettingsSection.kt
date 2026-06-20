package com.aozijx.passly.ui.features.settings.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup

@Composable
fun DataSettingsSection(
    isAutoDownloadIcons: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "数据与下载")
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = Icons.Default.CloudDownload,
            title = "下载图标",
            subtitle = "自动获取网站图标",
            checked = isAutoDownloadIcons,
            onCheckedChange = onAutoDownloadIconsChange
        )
    }
}