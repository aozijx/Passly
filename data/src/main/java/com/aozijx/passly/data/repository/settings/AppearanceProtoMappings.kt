package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.AppearancePreferences
import com.aozijx.passly.data.preferences.proto.FontFamilyPreference
import com.aozijx.passly.data.preferences.proto.ThemeModePreference
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode

internal fun ThemeModePreference.toDomain(): ThemeMode = when (this) {
    ThemeModePreference.THEME_MODE_SYSTEM -> ThemeMode.SYSTEM
    ThemeModePreference.THEME_MODE_LIGHT -> ThemeMode.LIGHT
    ThemeModePreference.THEME_MODE_DARK -> ThemeMode.DARK
    ThemeModePreference.THEME_MODE_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported theme mode: $this")
}

internal fun ThemeMode.toProto(): ThemeModePreference = when (this) {
    ThemeMode.SYSTEM -> ThemeModePreference.THEME_MODE_SYSTEM
    ThemeMode.LIGHT -> ThemeModePreference.THEME_MODE_LIGHT
    ThemeMode.DARK -> ThemeModePreference.THEME_MODE_DARK
}

internal fun FontFamilyPreference.toDomain(): FontFamilyMode = when (this) {
    FontFamilyPreference.FONT_FAMILY_SYSTEM -> FontFamilyMode.SYSTEM
    FontFamilyPreference.FONT_FAMILY_APP_BUNDLED -> FontFamilyMode.APP_BUNDLED
    FontFamilyPreference.FONT_FAMILY_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported font family: $this")
}

internal fun FontFamilyMode.toProto(): FontFamilyPreference = when (this) {
    FontFamilyMode.SYSTEM -> FontFamilyPreference.FONT_FAMILY_SYSTEM
    FontFamilyMode.APP_BUNDLED -> FontFamilyPreference.FONT_FAMILY_APP_BUNDLED
}

internal fun readAppearance(p: AppearancePreferences): AppearanceSettings =
    AppearanceSettings(
        themeMode = p.themeMode.toDomain(),
        isDynamicColor = p.dynamicColorEnabled,
        themeKey = p.themeKey,
        language = AppLanguage.fromLanguageTag(p.language),
        fontFamily = p.fontFamily.toDomain(),
    )
