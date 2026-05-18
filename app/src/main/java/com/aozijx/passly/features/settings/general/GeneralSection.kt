package com.aozijx.passly.features.settings.general

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.shell.ClickableSettingItem
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle

@Composable
fun CacheSettingsSection(
    cacheSize: String,
    onClearCache: () -> Unit
) {
    SettingsGroupTitle(text = "缓存")
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.DeleteSweep,
            title = "清除缓存",
            longValue = cacheSize,
            onClick = onClearCache
        )
    }
}

@Composable
fun AboutSettingsSection(
    appVersion: String,
    onAboutClick: () -> Unit
) {
    SettingsGroupTitle(text = "关于")
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.Info,
            title = "关于 Passly",
            longValue = "版本 $appVersion",
            onClick = onAboutClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            title = "用户协议",
            value = null,
            onClick = onAboutClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            title = "隐私政策",
            value = null,
            onClick = onAboutClick
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            title = "开源许可",
            value = null,
            onClick = onAboutClick
        )
    }
}