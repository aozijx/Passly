package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.ThemeMode as ProtoThemeMode
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint
import com.aozijx.passly.domain.settings.model.ThemeMode

// -- ThemeMode --
internal fun ProtoThemeMode.toDomain(): ThemeMode = when (this) {
    ProtoThemeMode.THEME_MODE_SYSTEM -> ThemeMode.SYSTEM
    ProtoThemeMode.THEME_MODE_LIGHT -> ThemeMode.LIGHT
    ProtoThemeMode.THEME_MODE_DARK -> ThemeMode.DARK
}

internal fun ThemeMode.toProto(): ProtoThemeMode = when (this) {
    ThemeMode.SYSTEM -> ProtoThemeMode.THEME_MODE_SYSTEM
    ThemeMode.LIGHT -> ProtoThemeMode.THEME_MODE_LIGHT
    ThemeMode.DARK -> ProtoThemeMode.THEME_MODE_DARK
}

// -- AppLanguage --
internal fun String.toAppLanguageDomain(): AppLanguage = AppLanguage.fromLanguageTag(this)

// -- FontFamilyMode --
internal fun String.toFontFamilyDomain(): FontFamilyMode = when (this) {
    "system" -> FontFamilyMode.SYSTEM
    "app.default" -> FontFamilyMode.APP_BUNDLED
    else -> FontFamilyMode.APP_BUNDLED
}

internal fun FontFamilyMode.toFontFamilyString(): String = when (this) {
    FontFamilyMode.SYSTEM -> "system"
    FontFamilyMode.APP_BUNDLED -> "app.default"
}

internal fun readAppearance(p: AppearancePreferences): AppearanceSettings =
    AppearanceSettings(
        themeMode = p.themeMode.toDomain(),
        isDynamicColor = p.dynamicColorEnabled,
        themeKey = p.themeKey,
        canvasTintPercent = p.canvasTintPercent.coerceIn(
            ThemeCanvasTint.MIN_PERCENT,
            ThemeCanvasTint.MAX_PERCENT,
        ),
        language = p.language.toAppLanguageDomain(),
        fontFamily = p.fontFamily.toFontFamilyDomain()
    )
