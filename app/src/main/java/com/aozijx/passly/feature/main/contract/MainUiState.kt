package com.aozijx.passly.feature.main.contract

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode

data class MainUiState(
    val isAuthorized: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val customSeedArgb: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val isExpressive: Boolean = true,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null
)
