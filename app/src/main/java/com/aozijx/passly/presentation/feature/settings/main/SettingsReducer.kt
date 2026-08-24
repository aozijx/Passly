package com.aozijx.passly.presentation.feature.settings.main

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState

internal sealed interface SettingsMutation {
    data class AppPasswordAvailabilityChanged(val enabled: Boolean) : SettingsMutation
    data object LoadingStarted : SettingsMutation
    data class SettingsLoaded(
        val swipeLeftAction: SwipeActionType,
        val swipeRightAction: SwipeActionType,
    ) : SettingsMutation
    data object LoadingFailed : SettingsMutation
    data class SwipeLeftActionSaved(val action: SwipeActionType) : SettingsMutation
    data class SwipeRightActionSaved(val action: SwipeActionType) : SettingsMutation
    data object DatabaseClearStarted : SettingsMutation
    data object DatabaseClearFinished : SettingsMutation
}

internal object SettingsReducer {
    fun reduce(state: SettingsUiState, mutation: SettingsMutation): SettingsUiState =
        when (mutation) {
            is SettingsMutation.AppPasswordAvailabilityChanged ->
                state.copy(isAppPasswordEnabled = mutation.enabled)
            SettingsMutation.LoadingStarted -> state.copy(isLoading = true)
            is SettingsMutation.SettingsLoaded -> state.copy(
                swipeLeftAction = mutation.swipeLeftAction,
                swipeRightAction = mutation.swipeRightAction,
                isLoading = false,
            )
            SettingsMutation.LoadingFailed -> state.copy(isLoading = false)
            is SettingsMutation.SwipeLeftActionSaved ->
                state.copy(swipeLeftAction = mutation.action)
            is SettingsMutation.SwipeRightActionSaved ->
                state.copy(swipeRightAction = mutation.action)
            SettingsMutation.DatabaseClearStarted -> state.copy(isClearingDatabase = true)
            SettingsMutation.DatabaseClearFinished -> state.copy(isClearingDatabase = false)
        }
}
