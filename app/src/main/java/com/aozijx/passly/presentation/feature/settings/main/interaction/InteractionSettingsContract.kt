package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.ui.interaction.InteractionDetailUiModel
import com.aozijx.passly.presentation.mapping.gesture.toUiModel
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel

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
    data class SetSwipeLeftAction(val action: SwipeActionUiModel) : InteractionSettingsAction
    data class SetSwipeRightAction(val action: SwipeActionUiModel) : InteractionSettingsAction
}

sealed interface InteractionSettingsEffect {
    data object Saved : InteractionSettingsEffect
    data class SaveFailed(val message: String) : InteractionSettingsEffect
}

internal fun InteractionSettingsUiState.Ready.toUiModel() = InteractionDetailUiModel(
    isSwipeEnabled = isSwipeEnabled,
    isPullToSearchEnabled = isPullToSearchEnabled,
    swipeLeftAction = swipeLeftAction.toUiModel(),
    swipeRightAction = swipeRightAction.toUiModel(),
)
