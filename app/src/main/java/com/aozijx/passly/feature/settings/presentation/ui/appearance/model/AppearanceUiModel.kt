package com.aozijx.passly.feature.settings.presentation.ui.appearance.model

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode

data class AppearanceUiModel(
    val themeMode: ThemeMode,
    val isDynamicColor: Boolean,
    val themeKey: String,
    val language: AppLanguage,
    val languageLabel: String,
    val languageOptions: List<LanguageOptionUiModel>,
    val fontFamily: FontFamilyMode,
)
