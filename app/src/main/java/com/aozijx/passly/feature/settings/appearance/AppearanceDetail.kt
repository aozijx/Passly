package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.core.ui.theme.themePresetByColor
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceDetail(
    state: AppearanceUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCustomSeedArgbChange: (Long?) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onFontFamilyChange: (FontFamilyMode) -> Unit
) {
    var showThemeColorSheet by remember { mutableStateOf(false) }
    var showThemeModeMenu by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
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
                        ThemeMode.SYSTEM to stringResource(R.string.follow_system),
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
                    subtitle = stringResource(R.string.settings_dynamic_color_desc),
                    checked = state.isDynamicColor,
                    onCheckedChange = onDynamicColorChange
                ),
                navigationSettingsGroupItem(
                    key = "appearance.theme_color",
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_color),
                    value = stringResource(themePresetByColor(state.customSeedArgb ?: 0L).nameKey),
                    onClick = { showThemeColorSheet = !showThemeColorSheet }
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
                    value = stringResource(state.language.labelRes()),
                    onClick = { showLanguageSheet = true }
                ),
                switchSettingsGroupItem(
                    key = "appearance.font",
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.settings_font),
                    subtitle = stringResource(R.string.settings_font_system),
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
        LanguageSheet(
            current = state.language,
            onSelect = { lang ->
                scope.launch {
                    // 1. 先触发 Sheet 的退出动画
                    sheetState.hide()
                    showLanguageSheet = false
                    // 2. 增加延迟（300ms），确保动画彻底完成，且主线程空闲
                    delay(300)
                    // 3. 此时再触发会导致 Activity 重启的语言变更
                    onLanguageChange(lang)
                }
            },
            sheetState = sheetState,
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showThemeColorSheet) {
        ThemeColorSheet(
            selectedColor = state.customSeedArgb ?: 0L,
            sheetState = sheetState,
            onSelect = { color ->
                onCustomSeedArgbChange(if (color == 0L) null else color)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showThemeColorSheet = false
                }
            },
            onDismiss = { showThemeColorSheet = false }
        )
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.follow_system
    ThemeMode.LIGHT -> R.string.settings_theme_mode_light
    ThemeMode.DARK -> R.string.settings_theme_mode_dark
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.follow_system
    AppLanguage.ZH -> R.string.settings_language_chinese
    AppLanguage.EN -> R.string.settings_language_english
    AppLanguage.JA -> R.string.settings_language_japanese
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // 使用 LazyColumn 替代 Column + verticalScroll 获得更稳定的滑动性能
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentPadding = PaddingValues(top = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_language_choose),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(AppLanguage.entries) { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = lang == current,
                        onClick = { onSelect(lang) }
                    )
                    Spacer(Modifier.padding(start = 12.dp))
                    Text(text = stringResource(lang.labelRes()))
                }
            }
        }
    }
}
