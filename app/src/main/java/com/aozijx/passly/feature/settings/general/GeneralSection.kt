package com.aozijx.passly.feature.settings.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
fun CacheSettingsSection(
    cacheSize: String,
    onClearCache: () -> Unit
) {
    SettingsSectionTitle(text = "缓存")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "general.clear_cache",
                icon = Icons.Default.DeleteSweep,
                title = "清除缓存",
                value = cacheSize,
                onClick = onClearCache
            )
        )
    )
}

@Composable
fun AboutSettingsSection(
    appVersion: String,
    onAboutClick: () -> Unit
) {
    SettingsSectionTitle(text = "关于")
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "general.about",
                icon = Icons.Default.Info,
                title = "关于 Passly",
                value = appVersion,
                onClick = onAboutClick
            ),
            navigationSettingsGroupItem(
                key = "general.terms",
                iconPlaceholder = true,
                title = "用户协议",
                onClick = onAboutClick
            ),
            navigationSettingsGroupItem(
                key = "general.privacy_policy",
                iconPlaceholder = true,
                title = "隐私政策",
                onClick = onAboutClick
            ),
            navigationSettingsGroupItem(
                key = "general.open_source",
                iconPlaceholder = true,
                title = "开源许可",
                onClick = onAboutClick
            )
        )
    )
}
