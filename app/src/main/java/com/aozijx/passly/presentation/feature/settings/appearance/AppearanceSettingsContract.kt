package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint

data class AppearanceSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val themeKey: String = "default",
    val canvasTintPercent: Int = ThemeCanvasTint.DEFAULT_PERCENT,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED
)

sealed interface AppearanceSettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : AppearanceSettingsAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceSettingsAction
    data class SetThemeKey(val key: String) : AppearanceSettingsAction
    data class SetCanvasTintPercent(val percent: Int) : AppearanceSettingsAction
    data class SetLanguage(val language: AppLanguage) : AppearanceSettingsAction
    data class SetFontFamily(val mode: FontFamilyMode) : AppearanceSettingsAction
}
