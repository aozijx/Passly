package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint
import com.aozijx.passly.presentation.ui.settings.appearance.model.AppearanceUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.FontFamilyUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.LanguageOptionUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.ThemeModeUiModel

/**
 * 语言选择器使用语言自称（中文 / English / 日本語），不会随当前界面语言二次翻译。
 */
@Composable
fun AppLanguage.localizedDisplayName(): String {
    if (this == AppLanguage.SYSTEM) return stringResource(R.string.settings_follow_system)

    val languageLocale = locale ?: return name
    val displayName = languageLocale.getDisplayName(languageLocale)
    return displayName.replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(languageLocale) else first.toString()
    }
}

@Composable
fun languagePickerOptions(): List<LanguageOptionUiModel> = AppLanguage.entries.map { language ->
    LanguageOptionUiModel(language.name, language.localizedDisplayName())
}

fun appLanguageFromKey(key: String): AppLanguage = AppLanguage.valueOf(key)

@Composable
fun AppearanceSettingsUiState.toAppearanceUiModel(): AppearanceUiModel = AppearanceUiModel(
    themeMode = themeMode.toUiModel(),
    isDynamicColor = isDynamicColor,
    themeKey = themeKey,
    canvasTintPercent = canvasTintPercent,
    canvasTintMinPercent = ThemeCanvasTint.MIN_PERCENT,
    canvasTintMaxPercent = ThemeCanvasTint.MAX_PERCENT,
    languageKey = language.name,
    languageLabel = language.localizedDisplayName(),
    languageOptions = languagePickerOptions(),
    fontFamily = fontFamily.toUiModel(),
)

fun ThemeMode.toUiModel(): ThemeModeUiModel = when (this) {
    ThemeMode.SYSTEM -> ThemeModeUiModel.SYSTEM
    ThemeMode.LIGHT -> ThemeModeUiModel.LIGHT
    ThemeMode.DARK -> ThemeModeUiModel.DARK
}

fun ThemeModeUiModel.toDomainModel(): ThemeMode = when (this) {
    ThemeModeUiModel.SYSTEM -> ThemeMode.SYSTEM
    ThemeModeUiModel.LIGHT -> ThemeMode.LIGHT
    ThemeModeUiModel.DARK -> ThemeMode.DARK
}

fun FontFamilyMode.toUiModel(): FontFamilyUiModel = when (this) {
    FontFamilyMode.APP_BUNDLED -> FontFamilyUiModel.APP_BUNDLED
    FontFamilyMode.SYSTEM -> FontFamilyUiModel.SYSTEM
}

fun FontFamilyUiModel.toDomainModel(): FontFamilyMode = when (this) {
    FontFamilyUiModel.APP_BUNDLED -> FontFamilyMode.APP_BUNDLED
    FontFamilyUiModel.SYSTEM -> FontFamilyMode.SYSTEM
}
