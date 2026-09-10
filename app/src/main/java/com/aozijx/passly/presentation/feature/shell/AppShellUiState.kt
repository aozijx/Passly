package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint
import com.aozijx.passly.domain.settings.model.ThemeMode

data class AppShellUiState(
    val isAuthorized: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val themeKey: String = "default",
    val canvasTintPercent: Int = ThemeCanvasTint.DEFAULT_PERCENT,
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null
)
