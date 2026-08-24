package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.presentation.feature.shell.AppShellUiState

internal sealed interface AppShellMutation {
    data object Authenticated : AppShellMutation
    data object RecoveryModeEntered : AppShellMutation
    data object SessionLocked : AppShellMutation
    data class SettingsChanged(
        val appearance: AppearanceSettings,
        val interfaceSettings: InterfaceSettings,
    ) : AppShellMutation
    data class DatabaseInitializationStarted(val clearError: Boolean) : AppShellMutation
    data class DatabaseInitializationFinished(val error: Throwable?) : AppShellMutation
    data object DatabaseInitializationStopped : AppShellMutation
    data class DatabaseFailureObserved(val error: Throwable) : AppShellMutation
}

internal object AppShellReducer {
    fun reduce(state: AppShellUiState, mutation: AppShellMutation): AppShellUiState =
        when (mutation) {
            AppShellMutation.Authenticated -> state.copy(
                isAuthorized = true,
                isRecoveryMode = false,
            )
            AppShellMutation.RecoveryModeEntered -> state.copy(
                isAuthorized = false,
                isRecoveryMode = true,
                isDatabaseInitializing = false,
                databaseError = null,
            )
            AppShellMutation.SessionLocked -> state.copy(
                isAuthorized = false,
                isRecoveryMode = false,
            )
            is AppShellMutation.SettingsChanged -> state.copy(
                themeMode = mutation.appearance.themeMode,
                isDynamicColor = mutation.appearance.isDynamicColor,
                themeKey = mutation.appearance.themeKey,
                canvasTintPercent = mutation.appearance.canvasTintPercent,
                fontFamily = mutation.appearance.fontFamily,
                language = mutation.appearance.language,
                outerCornerRadiusDp = mutation.interfaceSettings.outerCornerRadiusDp,
                innerCornerRadiusDp = mutation.interfaceSettings.innerCornerRadiusDp,
                groupItemSpacingDp = mutation.interfaceSettings.groupItemSpacingDp,
                groupContentPaddingDp = mutation.interfaceSettings.groupContentPaddingDp,
            )
            is AppShellMutation.DatabaseInitializationStarted -> state.copy(
                isDatabaseInitializing = true,
                databaseError = state.databaseError.takeUnless { mutation.clearError },
            )
            is AppShellMutation.DatabaseInitializationFinished -> state.copy(
                isDatabaseInitializing = false,
                databaseError = mutation.error,
            )
            AppShellMutation.DatabaseInitializationStopped ->
                state.copy(isDatabaseInitializing = false)
            is AppShellMutation.DatabaseFailureObserved -> state.copy(
                isDatabaseInitializing = false,
                databaseError = mutation.error,
                isAuthorized = false,
            )
        }
}
