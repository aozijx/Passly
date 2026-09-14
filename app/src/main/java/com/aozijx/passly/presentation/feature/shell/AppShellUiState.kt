package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints

data class AppShellUiState(
    val isAuthorized: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    val isDatabaseInitializing: Boolean = false,
    val databaseError: Throwable? = null
)
