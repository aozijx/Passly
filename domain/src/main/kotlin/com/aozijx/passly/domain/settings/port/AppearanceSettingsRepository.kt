package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppearanceSettingsRepository {
    val appearance: Flow<AppearanceSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setThemeKey(key: String)
    suspend fun setCanvasTintPercent(percent: Int)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setFontFamily(mode: FontFamilyMode)
}
