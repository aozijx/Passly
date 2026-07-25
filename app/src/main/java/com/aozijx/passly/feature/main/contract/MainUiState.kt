package com.aozijx.passly.feature.main.contract

import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode

data class MainUiState(
    val isAuthorized: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val customSeedArgb: Long? = null,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null
)
