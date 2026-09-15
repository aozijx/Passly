package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints

data class AppShellUiState(
    val isAuthorized: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    val isDatabaseRetrying: Boolean = false,
    val databaseError: Throwable? = null
)

internal enum class AppShellDestination {
    DATABASE_ERROR,
    VAULT,
    RECOVERY,
    AUTHENTICATION,
}

internal val AppShellUiState.destination: AppShellDestination
    get() = when {
        databaseError != null -> AppShellDestination.DATABASE_ERROR
        isAuthorized -> AppShellDestination.VAULT
        isRecoveryMode -> AppShellDestination.RECOVERY
        else -> AppShellDestination.AUTHENTICATION
    }
