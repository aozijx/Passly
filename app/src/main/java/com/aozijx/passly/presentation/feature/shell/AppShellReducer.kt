package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings

internal sealed interface AppShellMutation {
    data class SessionChanged(val mode: AppShellSessionMode) : AppShellMutation
    data class SettingsChanged(
        val appearance: AppearanceSettings,
        val interfaceSettings: InterfaceSettings,
        val securitySettings: SecuritySettings,
    ) : AppShellMutation
    data object DatabaseRetryStarted : AppShellMutation
    data object DatabaseRetryFinished : AppShellMutation
    data class DatabaseFailureChanged(val error: Throwable?) : AppShellMutation
}

internal object AppShellReducer {
    fun reduce(state: AppShellUiState, mutation: AppShellMutation): AppShellUiState =
        when (mutation) {
            is AppShellMutation.SessionChanged -> state.copy(sessionMode = mutation.mode)
            is AppShellMutation.SettingsChanged -> state.copy(
                appearance = mutation.appearance,
                appCornerRadiusDp = mutation.interfaceSettings.appCornerRadiusDp,
                windowPolicy = AppWindowPolicy(
                    isSecureContentEnabled = mutation.securitySettings.isSecureContentEnabled,
                    isStatusBarAutoHide = mutation.interfaceSettings.hideSystemBars,
                ),
            )
            AppShellMutation.DatabaseRetryStarted -> state.copy(
                isDatabaseRetrying = true,
            )
            AppShellMutation.DatabaseRetryFinished -> state.copy(
                isDatabaseRetrying = false,
            )
            is AppShellMutation.DatabaseFailureChanged -> state.copy(
                isDatabaseRetrying = false,
                databaseError = mutation.error,
            )
        }
}
