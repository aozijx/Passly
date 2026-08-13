package com.aozijx.passly.feature.settings.appearance

import com.aozijx.passly.data.settings.model.AppLanguage
import com.aozijx.passly.data.settings.model.FontFamilyMode
import com.aozijx.passly.data.settings.model.ThemeMode

data class AppearanceSettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val manualThemeColorArgb: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED
)

sealed interface AppearanceSettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : AppearanceSettingsAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceSettingsAction
    data class SelectManualThemeColor(val argb: Long?) : AppearanceSettingsAction
    data class SetLanguage(val language: AppLanguage) : AppearanceSettingsAction
    data class SetFontFamily(val mode: FontFamilyMode) : AppearanceSettingsAction
}
