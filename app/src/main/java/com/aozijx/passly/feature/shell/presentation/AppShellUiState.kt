package com.aozijx.passly.feature.shell.presentation

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.settings.model.AppearanceSettings

data class AppShellUiState(
    val sessionMode: AppShellSessionMode = AppShellSessionMode.AUTHENTICATION,
    val settings: AppShellSettingsState = AppShellSettingsState.Loading,
    val databaseState: AppShellDatabaseState = AppShellDatabaseState.Available,
)

sealed interface AppShellSettingsState {
    data object Loading : AppShellSettingsState

    data class Ready(
        val appearance: AppearanceSettings,
        val appCornerRadiusDp: Float,
        val windowPolicy: AppWindowPolicy,
    ) : AppShellSettingsState
}

internal val AppShellUiState.windowPolicy: AppWindowPolicy
    get() = (settings as? AppShellSettingsState.Ready)?.windowPolicy ?: AppWindowPolicy()

sealed interface AppShellDatabaseState {
    data object Available : AppShellDatabaseState
    data class Failed(
        val cause: Throwable,
        val retryPhase: DatabaseRetryPhase = DatabaseRetryPhase.Idle,
    ) : AppShellDatabaseState
}

enum class DatabaseRetryPhase {
    Idle,
    Retrying,
}

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
        databaseState is AppShellDatabaseState.Failed -> AppShellDestination.DATABASE_ERROR
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
    is AuthenticationState.Locking,
    is AuthenticationState.AutofillAuthenticated -> AppShellSessionMode.AUTHENTICATION
}
