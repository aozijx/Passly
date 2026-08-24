package com.aozijx.passly.presentation.ui.settings.appearance.model

data class AppearanceUiModel(
    val themeMode: ThemeModeUiModel,
    val isDynamicColor: Boolean,
    val themeKey: String,
    val canvasTintPercent: Int,
    val canvasTintMinPercent: Int,
    val canvasTintMaxPercent: Int,
    val languageKey: String,
    val languageLabel: String,
    val languageOptions: List<LanguageOptionUiModel>,
    val fontFamily: FontFamilyUiModel,
)

enum class ThemeModeUiModel {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class FontFamilyUiModel {
    APP_BUNDLED,
    SYSTEM,
}
