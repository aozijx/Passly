package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType

sealed interface InteractionSettingsUiState {
    data object Loading : InteractionSettingsUiState

    data class Ready(
        val isSwipeEnabled: Boolean,
        val isPullToSearchEnabled: Boolean,
        val swipeLeftAction: SwipeActionType,
        val swipeRightAction: SwipeActionType,
    ) : InteractionSettingsUiState
}

sealed interface InteractionSettingsAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetPullToSearchEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetSwipeLeftAction(val action: SwipeActionType) : InteractionSettingsAction
    data class SetSwipeRightAction(val action: SwipeActionType) : InteractionSettingsAction
}

sealed interface InteractionSettingsEffect {
    data object Saved : InteractionSettingsEffect
    data class SaveFailed(val message: String) : InteractionSettingsEffect
}
