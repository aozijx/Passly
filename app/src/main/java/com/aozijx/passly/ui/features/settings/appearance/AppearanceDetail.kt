package com.aozijx.passly.ui.features.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.internal.SettingsContentActions
import com.aozijx.passly.ui.features.settings.internal.SettingsContentState
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun AppearanceDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGroupTitle(text = "主题")
        SettingsRoundedGroup {
            switchSettingsItem(
                icon = if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "深色模式",
                subtitle = if (state.isDarkMode) "当前为深色主题" else "当前为浅色主题",
                checked = state.isDarkMode,
                onCheckedChange = actions.onDarkModeChange
            )
            switchSettingsItem(
                icon = Icons.Default.Palette,
                title = "动态取色",
                subtitle = "使用系统壁纸颜色作为应用主题",
                checked = state.isDynamicColor,
                onCheckedChange = actions.onDynamicColorChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupTitle(text = "语言")
        SettingsRoundedGroup {
            navigationSettingsItem(
                icon = Icons.Default.Language,
                title = "应用语言",
                value = "简体中文",
                onClick = {}
            )
        }
    }
}
