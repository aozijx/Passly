package com.aozijx.passly.features.settings.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SwitchSettingItem

@Composable
fun DarkModeSettingsSection(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "主题模式")
    SettingsCard {
        SwitchSettingItem(
            icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
            title = "深色模式",
            subtitle = if (isDarkMode) "当前为深色主题" else "当前为浅色主题",
            checked = isDarkMode,
            onCheckedChange = onDarkModeChange
        )
    }
}