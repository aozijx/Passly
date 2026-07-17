package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.components.navigationSettingsItem
import com.aozijx.passly.feature.settings.components.switchSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup
import com.aozijx.passly.feature.settings.shell.sectionSpacing
import com.aozijx.passly.ui.theme.themePresetByColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceDetail(
    state: AppearanceUiState,
    onDarkModeChange: (Boolean?) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeColorChange: (Long) -> Unit
) {
    var showThemeColorSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // 预加载资源字符串
    val settingsGroupTheme = stringResource(R.string.settings_group_theme)
    val settingsDarkMode = stringResource(R.string.settings_dark_mode)
    val settingsDarkModeOn = stringResource(R.string.settings_dark_mode_on)
    val settingsDarkModeOff = stringResource(R.string.settings_dark_mode_off)
    val settingsDynamicColor = stringResource(R.string.settings_dynamic_color)
    val settingsDynamicColorDesc = stringResource(R.string.settings_dynamic_color_desc)
    val settingsThemeColor = stringResource(R.string.settings_theme_color)

    // 预加载主题色标签
    val themeColorLabel = stringResource(themePresetByColor(state.themeColor).nameKey)

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
            navigationSettingsItem(
                icon = Icons.Default.Palette,
                title = settingsThemeColor,
                value = themeColorLabel,
                onClick = { showThemeColorSheet = true }
            )
        }

    }

    if (showThemeColorSheet) {
        ThemeColorSheet(
            selectedColor = state.themeColor,
            sheetState = sheetState,
            onSelect = { color ->
                onThemeColorChange(color)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showThemeColorSheet = false
                }
            },
            onDismiss = { showThemeColorSheet = false }
        )
    }
}
