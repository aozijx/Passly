package com.aozijx.passly.ui.features.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aozijx.passly.R
import com.aozijx.passly.core.utils.restartApp
import com.aozijx.passly.ui.features.settings.components.navigationSettingsItem
import com.aozijx.passly.ui.features.settings.components.switchSettingsItem
import com.aozijx.passly.ui.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.ui.features.settings.shell.SettingsRoundedGroup
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun AppearanceDetail(
    state: AppearanceUiState,
    onDarkModeChange: (Boolean?) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 预加载资源字符串
    val settingsGroupTheme = stringResource(R.string.settings_group_theme)
    val settingsDarkMode = stringResource(R.string.settings_dark_mode)
    val settingsDarkModeOn = stringResource(R.string.settings_dark_mode_on)
    val settingsDarkModeOff = stringResource(R.string.settings_dark_mode_off)
    val settingsDynamicColor = stringResource(R.string.settings_dynamic_color)
    val settingsDynamicColorDesc = stringResource(R.string.settings_dynamic_color_desc)
    val settingsGroupLanguage = stringResource(R.string.settings_group_language)
    val settingsAppLanguage = stringResource(R.string.settings_app_language)
    val languageFollowSystem = stringResource(R.string.language_follow_system)

    // 预加载语言标签映射
    val languageLabelMap = languageOptions.associate { option ->
        option.code to stringResource(option.labelRes)
    }

    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))
        val isDarkMode = state.isDarkMode == true

        SettingsGroupTitle(text = settingsGroupTheme)
        SettingsRoundedGroup {
            switchSettingsItem(
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = settingsDarkMode,
                subtitle = if (isDarkMode) settingsDarkModeOn else settingsDarkModeOff,
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange
            )
            switchSettingsItem(
                icon = Icons.Default.Palette,
                title = settingsDynamicColor,
                subtitle = settingsDynamicColorDesc,
                checked = state.isDynamicColor,
                onCheckedChange = onDynamicColorChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupTitle(text = settingsGroupLanguage)
        SettingsRoundedGroup {
            val languageLabel = languageLabelMap[state.languageCode] ?: languageFollowSystem
            navigationSettingsItem(
                icon = Icons.Default.Language,
                title = settingsAppLanguage,
                value = languageLabel,
                onClick = { showLanguageDialog = true }
            )
        }
    }

    if (showLanguageDialog) {
        LanguageSelectDialog(
            selectedCode = state.languageCode,
            onSelect = { code ->
                if (code != state.languageCode) {
                    onLanguageChange(code)
                    showRestartDialog = true
                }
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* 外部不可点击，此回调不会触发，但必须提供非空 lambda */ },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            ),
            title = { Text(text = stringResource(R.string.language_restart_title)) },
            text = { Text(text = stringResource(R.string.language_restart_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        context.restartApp()
                    }
                ) {
                    Text(text = stringResource(R.string.language_restart_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestartDialog = false }
                ) {
                    Text(text = stringResource(R.string.language_restart_later))
                }
            }
        )
    }
}