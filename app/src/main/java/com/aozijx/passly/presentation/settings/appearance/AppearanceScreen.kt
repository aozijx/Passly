package com.aozijx.passly.presentation.settings.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
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
import com.aozijx.passly.feature.settings.appearance.localizedDisplayName
import com.aozijx.passly.feature.settings.appearance.labelRes
import com.aozijx.passly.feature.settings.appearance.AppearanceSettingsUiState
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.presentation.settings.appearance.pickers.LanguagePicker
import com.aozijx.passly.presentation.settings.appearance.pickers.ThemePicker
import com.aozijx.passly.presentation.settings.appearance.pickers.labelRes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceDetail(
    state: AppearanceSettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onFallbackPaletteSelect: (FallbackPalette) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onFontFamilyChange: (FontFamilyMode) -> Unit
) {
    var showThemeColorSheet by remember { mutableStateOf(false) }
    var showThemeModeMenu by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val languageSheetState = rememberModalBottomSheetState()
    val themeColorSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionTitle(text = stringResource(R.string.settings_group_theme))
        RoundedGroup(
            items = listOf(
                dropdownSettingsGroupItem(
                    key = "appearance.theme_mode",
                    icon = when (state.themeMode) {
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    },
                    title = stringResource(R.string.settings_theme_mode),
                    selected = state.themeMode,
                    selectedLabel = stringResource(state.themeMode.labelRes()),
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_follow_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_mode_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_mode_dark)
                    ),
                    expanded = showThemeModeMenu,
                    onExpandedChange = { showThemeModeMenu = it },
                    onSelect = onThemeModeChange
                ),
                switchSettingsGroupItem(
                    key = "appearance.dynamic_color",
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_description),
                    checked = state.isDynamicColor,
                    onCheckedChange = onDynamicColorChange
                ),
                navigationSettingsGroupItem(
                    key = "appearance.theme_color",
                    enabled = !state.isDynamicColor,
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_color),
                    value = if (state.isDynamicColor) {
                        stringResource(R.string.settings_dynamic_color)
                    } else {
                        stringResource(state.fallbackPalette.labelRes())
                    },
                    onClick = { showThemeColorSheet = true }
                )
            )
        )

        Spacer(Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_topic_language_font))
        RoundedGroup(
            items = listOf(
                navigationSettingsGroupItem(
                    key = "appearance.language",
                    icon = Icons.Default.Translate,
                    title = stringResource(R.string.settings_language),
                    value = state.language.localizedDisplayName(),
                    onClick = { showLanguageSheet = true }
                ),
                switchSettingsGroupItem(
                    key = "appearance.font",
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.settings_font),
                    subtitle = stringResource(
                        if (state.fontFamily == FontFamilyMode.SYSTEM) {
                            R.string.settings_font_system
                        } else {
                            R.string.settings_font_app
                        }
                    ),
                    checked = state.fontFamily == FontFamilyMode.SYSTEM,
                    onCheckedChange = { useSystem ->
                        onFontFamilyChange(
                            if (useSystem) FontFamilyMode.SYSTEM else FontFamilyMode.APP_BUNDLED
                        )
                    }
                )
            )
        )
    }

    if (showLanguageSheet) {
        LanguagePicker(
            current = state.language,
            onSelect = { lang ->
                scope.launch { languageSheetState.hide() }.invokeOnCompletion {
                    showLanguageSheet = false
                    // 此时触发导致 Activity 重启的语言变更
                    onLanguageChange(lang)
                }
            },
            sheetState = languageSheetState,
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showThemeColorSheet && !state.isDynamicColor) {
        ThemePicker(
            selectedPalette = state.fallbackPalette,
            sheetState = themeColorSheetState,
            onSelect = { palette ->
                onFallbackPaletteSelect(palette)
                scope.launch { themeColorSheetState.hide() }.invokeOnCompletion {
                    showThemeColorSheet = false
                }
            },
            onDismiss = { showThemeColorSheet = false }
        )
    }
}
