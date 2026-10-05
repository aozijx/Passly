package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.AppearanceSettings

data class AppShellUiState(
    val sessionMode: AppShellSessionMode = AppShellSessionMode.AUTHENTICATION,
    val appearance: AppearanceSettings = AppearanceSettings(),
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    val windowPolicy: AppWindowPolicy = AppWindowPolicy(),
    val isDatabaseRetrying: Boolean = false,
    val databaseError: Throwable? = null
)

enum class AppShellSessionMode {
    AUTHENTICATION,
    VAULT,
    RECOVERY,
}

internal enum class AppShellDestination {
    DATABASE_ERROR,
    VAULT,
    RECOVERY,
    AUTHENTICATION,
}

internal val AppShellUiState.destination: AppShellDestination
    get() = when {
        databaseError != null -> AppShellDestination.DATABASE_ERROR
        sessionMode == AppShellSessionMode.VAULT -> AppShellDestination.VAULT
        sessionMode == AppShellSessionMode.RECOVERY -> AppShellDestination.RECOVERY
        else -> AppShellDestination.AUTHENTICATION
    }

internal fun AuthenticationState.toAppShellSessionMode(): AppShellSessionMode = when (this) {
    is AuthenticationState.Authenticated -> AppShellSessionMode.VAULT
    is AuthenticationState.RecoveryMode -> AppShellSessionMode.RECOVERY
    AuthenticationState.Locked,
    is AuthenticationState.AwaitingHost,
    is AuthenticationState.Authenticating,
    is AuthenticationState.Unlocking,
    is AuthenticationState.Locking -> AppShellSessionMode.AUTHENTICATION
}
