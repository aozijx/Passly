package com.aozijx.passly.presentation.ui.settings.appearance

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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
import com.aozijx.passly.presentation.ui.shared.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.core.ui.theme.AppThemeSchemes
import com.aozijx.passly.presentation.ui.settings.appearance.model.AppearanceUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.FontFamilyUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.ThemeModeUiModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceDetail(
    state: AppearanceUiModel,
    onThemeModeChange: (ThemeModeUiModel) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeKeySelect: (String) -> Unit,
    onCanvasTintPercentChange: (Int) -> Unit,
    onLanguageChange: (String) -> Unit,
    onFontFamilyChange: (FontFamilyUiModel) -> Unit
) {
    var showThemeColorSheet by remember { mutableStateOf(false) }
    var showThemeModeMenu by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val languageSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val themeColorSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()
    var canvasTintPercent by remember(state.canvasTintPercent) {
        mutableFloatStateOf(state.canvasTintPercent.toFloat())
    }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionTitle(text = stringResource(R.string.settings_group_theme))
        RoundedGroup(
            items = listOf(
                dropdownSettingsGroupItem(
                    key = "appearance.theme_mode",
                    icon = when (state.themeMode) {
                        ThemeModeUiModel.SYSTEM -> Icons.Default.SettingsBrightness
                        ThemeModeUiModel.LIGHT -> Icons.Default.LightMode
                        ThemeModeUiModel.DARK -> Icons.Default.DarkMode
                    },
                    title = stringResource(R.string.settings_theme_mode),
                    selected = state.themeMode,
                    selectedLabel = stringResource(state.themeMode.labelRes),
                    options = listOf(
                        ThemeModeUiModel.SYSTEM to stringResource(R.string.settings_follow_system),
                        ThemeModeUiModel.LIGHT to stringResource(R.string.settings_theme_mode_light),
                        ThemeModeUiModel.DARK to stringResource(R.string.settings_theme_mode_dark)
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
                    key = "appearance.theme_scheme",
                    enabled = !state.isDynamicColor,
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_scheme),
                    value = if (state.isDynamicColor) {
                        stringResource(R.string.settings_dynamic_color)
                    } else {
                        AppThemeSchemes.find(state.themeKey)?.let { scheme ->
                            stringResource(scheme.nameRes)
                        } ?: stringResource(R.string.settings_theme_scheme_default)
                    },
                    onClick = { showThemeColorSheet = true }
                ),
                sliderSettingsGroupItem(
                    key = "appearance.canvas_tint",
                    enabled = !state.isDynamicColor && state.themeKey.isNotBlank(),
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_canvas_tint),
                    subtitle = stringResource(R.string.settings_theme_canvas_tint_description),
                    value = canvasTintPercent,
                    valueLabel = stringResource(
                        R.string.settings_value_percent,
                        canvasTintPercent.roundToInt(),
                    ),
                    valueRange = state.canvasTintMinPercent.toFloat()..
                            state.canvasTintMaxPercent.toFloat(),
                    steps = state.canvasTintMaxPercent - state.canvasTintMinPercent - 1,
                    onValueChange = { canvasTintPercent = it },
                    onValueChangeFinished = {
                        val selectedPercent = canvasTintPercent.roundToInt()
                        if (selectedPercent != state.canvasTintPercent) {
                            onCanvasTintPercentChange(selectedPercent)
                        }
                    },
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
                    value = state.languageLabel,
                    onClick = { showLanguageSheet = true }
                ),
                switchSettingsGroupItem(
                    key = "appearance.font",
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.settings_font),
                    subtitle = stringResource(
                        if (state.fontFamily == FontFamilyUiModel.SYSTEM) {
                            R.string.settings_font_system
                        } else {
                            R.string.settings_font_app
                        }
                    ),
                    checked = state.fontFamily == FontFamilyUiModel.SYSTEM,
                    onCheckedChange = { useSystem ->
                        onFontFamilyChange(
                            if (useSystem) FontFamilyUiModel.SYSTEM else FontFamilyUiModel.APP_BUNDLED
                        )
                    }
                )
            )
        )
    }

    if (showLanguageSheet) {
        LanguagePicker(
            currentKey = state.languageKey,
            options = state.languageOptions,
            onSelect = { languageKey ->
                scope.launch { languageSheetState.hide() }.invokeOnCompletion {
                    showLanguageSheet = false
                    // 此时触发导致 Activity 重启的语言变更
                    onLanguageChange(languageKey)
                }
            },
            sheetState = languageSheetState,
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showThemeColorSheet && !state.isDynamicColor) {
        ThemePicker(
            selectedThemeKey = state.themeKey,
            sheetState = themeColorSheetState,
            onSelect = { key ->
                onThemeKeySelect(key)
                scope.launch { themeColorSheetState.hide() }.invokeOnCompletion {
                    showThemeColorSheet = false
                }
            },
            onDismiss = { showThemeColorSheet = false }
        )
    }
}

private val ThemeModeUiModel.labelRes: Int
    get() = when (this) {
        ThemeModeUiModel.SYSTEM -> R.string.settings_follow_system
        ThemeModeUiModel.LIGHT -> R.string.settings_theme_mode_light
        ThemeModeUiModel.DARK -> R.string.settings_theme_mode_dark
    }
