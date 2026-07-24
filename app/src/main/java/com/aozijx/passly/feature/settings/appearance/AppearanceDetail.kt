package com.aozijx.passly.feature.settings.appearance

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.core.ui.theme.themePresetByColor
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
    var showThemeModeMenu by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val languageTag = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .substringBefore(',')

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionTitle(text = stringResource(R.string.settings_group_theme))
        RoundedGroup(
            items = listOf(
                dropdownSettingsGroupItem(
                    key = "appearance.theme_mode",
                    icon = when (state.isDarkMode) {
                        true -> Icons.Default.DarkMode
                        false -> Icons.Default.LightMode
                        null -> Icons.Default.SettingsBrightness
                    },
                    title = stringResource(R.string.settings_theme_mode),
                    selected = state.isDarkMode,
                    selectedLabel = stringResource(state.isDarkMode.labelRes()),
                    options = listOf(
                        null to stringResource(R.string.follow_system),
                        false to stringResource(R.string.settings_theme_mode_light),
                        true to stringResource(R.string.settings_theme_mode_dark)
                    ),
                    expanded = showThemeModeMenu,
                    onExpandedChange = { showThemeModeMenu = it },
                    onSelect = onDarkModeChange
                ),
                navigationSettingsGroupItem(
                    key = "appearance.language",
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    value = stringResource(languageTag.languageLabelRes()),
                    onClick = { showLanguageDialog = true }
                ),
                switchSettingsGroupItem(
                    key = "appearance.dynamic_color",
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_desc),
                    checked = state.isDynamicColor,
                    onCheckedChange = onDynamicColorChange
                ),
                navigationSettingsGroupItem(
                    key = "appearance.theme_color",
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_color),
                    value = stringResource(themePresetByColor(state.themeColor).nameKey),
                    onClick = { showThemeColorSheet = true }
                )
            )
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language_choose),
            options = listOf(
                "" to stringResource(R.string.follow_system),
                "zh-CN" to stringResource(R.string.settings_language_chinese),
                "en" to stringResource(R.string.settings_language_english),
                "ja" to stringResource(R.string.settings_language_japanese)
            ),
            selected = languageTag,
            onSelect = { tag ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
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

private fun Boolean?.labelRes(): Int = when (this) {
    null -> R.string.follow_system
    false -> R.string.settings_theme_mode_light
    true -> R.string.settings_theme_mode_dark
}

private fun String.languageLabelRes(): Int = when {
    startsWith("zh", ignoreCase = true) -> R.string.settings_language_chinese
    startsWith("en", ignoreCase = true) -> R.string.settings_language_english
    startsWith("ja", ignoreCase = true) -> R.string.settings_language_japanese
    else -> R.string.follow_system
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
