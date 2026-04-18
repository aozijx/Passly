package com.aozijx.passly.features.settings.components.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle
import com.aozijx.passly.features.settings.components.common.SwitchSettingItem

@Composable
fun DataSettingsSection(
    isAutoDownloadIcons: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "数据与下载")
    SettingsCard {
        SwitchSettingItem(
            icon = Icons.Default.CloudDownload,
            title = "自动下载图标",
            subtitle = "在保险箱列表中自动获取网站图标",
            checked = isAutoDownloadIcons,
            onCheckedChange = onAutoDownloadIconsChange
        )
    }
}