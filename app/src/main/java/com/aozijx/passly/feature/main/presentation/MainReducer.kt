package com.aozijx.passly.feature.main.presentation

import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.feature.main.contract.MainUiState

internal sealed interface MainMutation {
    data object Authenticated : MainMutation
    data object RecoveryModeEntered : MainMutation
    data object SessionLocked : MainMutation
    data class SettingsChanged(
        val appearance: AppearanceSettings,
        val interfaceSettings: InterfaceSettings,
    ) : MainMutation
    data class DatabaseInitializationStarted(val clearError: Boolean) : MainMutation
    data class DatabaseInitializationFinished(val error: Throwable?) : MainMutation
    data object DatabaseInitializationStopped : MainMutation
    data class DatabaseFailureObserved(val error: Throwable) : MainMutation
}

internal object MainReducer {
    fun reduce(state: MainUiState, mutation: MainMutation): MainUiState =
        when (mutation) {
            MainMutation.Authenticated -> state.copy(
                isAuthorized = true,
                isRecoveryMode = false,
            )
            MainMutation.RecoveryModeEntered -> state.copy(
                isAuthorized = false,
                isRecoveryMode = true,
                isDatabaseInitializing = false,
                databaseError = null,
            )
            MainMutation.SessionLocked -> state.copy(
                isAuthorized = false,
                isRecoveryMode = false,
            )
            is MainMutation.SettingsChanged -> state.copy(
                themeMode = mutation.appearance.themeMode,
                isDynamicColor = mutation.appearance.isDynamicColor,
                manualThemeColorArgb = mutation.appearance.manualThemeColorArgb,
                fontFamily = mutation.appearance.fontFamily,
                language = mutation.appearance.language,
                outerCornerRadiusDp = mutation.interfaceSettings.outerCornerRadiusDp,
                innerCornerRadiusDp = mutation.interfaceSettings.innerCornerRadiusDp,
                groupItemSpacingDp = mutation.interfaceSettings.groupItemSpacingDp,
                groupContentPaddingDp = mutation.interfaceSettings.groupContentPaddingDp,
            )
            is MainMutation.DatabaseInitializationStarted -> state.copy(
                isDatabaseInitializing = true,
                databaseError = state.databaseError.takeUnless { mutation.clearError },
            )
            is MainMutation.DatabaseInitializationFinished -> state.copy(
                isDatabaseInitializing = false,
                databaseError = mutation.error,
            )
            MainMutation.DatabaseInitializationStopped ->
                state.copy(isDatabaseInitializing = false)
            is MainMutation.DatabaseFailureObserved -> state.copy(
                isDatabaseInitializing = false,
                databaseError = mutation.error,
                isAuthorized = false,
            )
        }
}
