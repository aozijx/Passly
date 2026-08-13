package com.aozijx.passly.app.shell.contract

import com.aozijx.passly.data.settings.model.AppLanguage
import com.aozijx.passly.data.settings.model.FontFamilyMode
import com.aozijx.passly.data.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.data.settings.model.ThemeMode

data class AppShellUiState(
    val isAuthorized: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val manualThemeColorArgb: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    val innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    val groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    val groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null
)
