package com.aozijx.passly.feature.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.components.navigationSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
fun CacheSettingsSection(
    cacheSize: String,
    onClearCache: () -> Unit
) {
    SettingsGroupTitle(text = "缓存")
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.DeleteSweep,
            title = "清除缓存",
            value = cacheSize,
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
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.Info,
            title = "关于 Passly",
            value = appVersion,
            onClick = onAboutClick
        )
        navigationSettingsItem(
            iconPlaceholder = true,
            title = "用户协议",
            onClick = onAboutClick
        )
        navigationSettingsItem(
            iconPlaceholder = true,
            title = "隐私政策",
            onClick = onAboutClick
        )
        navigationSettingsItem(
            iconPlaceholder = true,
            title = "开源许可",
            onClick = onAboutClick
        )
    }
}