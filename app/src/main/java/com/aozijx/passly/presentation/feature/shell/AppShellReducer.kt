package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings

internal sealed interface AppShellMutation {
    data object Authenticated : AppShellMutation
    data object RecoveryModeEntered : AppShellMutation
    data object SessionLocked : AppShellMutation
    data class SettingsChanged(
        val appearance: AppearanceSettings,
        val interfaceSettings: InterfaceSettings,
    ) : AppShellMutation
    data object DatabaseRetryStarted : AppShellMutation
    data class DatabaseRetryFinished(val error: Throwable?) : AppShellMutation
    data class DatabaseFailureObserved(val error: Throwable) : AppShellMutation
}

internal object AppShellReducer {
    fun reduce(state: AppShellUiState, mutation: AppShellMutation): AppShellUiState =
        when (mutation) {
            AppShellMutation.Authenticated -> state.copy(
                isAuthorized = true,
                isRecoveryMode = false,
                isDatabaseRetrying = false,
                databaseError = null,
            )
            AppShellMutation.RecoveryModeEntered -> state.copy(
                isAuthorized = false,
                isRecoveryMode = true,
                isDatabaseRetrying = false,
                databaseError = null,
            )
            AppShellMutation.SessionLocked -> state.copy(
                isAuthorized = false,
                isRecoveryMode = false,
            )
            is AppShellMutation.SettingsChanged -> state.copy(
                appearance = mutation.appearance,
                appCornerRadiusDp = mutation.interfaceSettings.appCornerRadiusDp,
            )
            AppShellMutation.DatabaseRetryStarted -> state.copy(
                isDatabaseRetrying = true,
                databaseError = null,
            )
            is AppShellMutation.DatabaseRetryFinished -> state.copy(
                isDatabaseRetrying = false,
                databaseError = mutation.error,
            )
            is AppShellMutation.DatabaseFailureObserved -> state.copy(
                isDatabaseRetrying = false,
                databaseError = mutation.error,
                isAuthorized = false,
            )
        }
}