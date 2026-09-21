package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.feature.settings.ui.interaction.InteractionDetailUiModel
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel

data class InteractionSettingsUiState(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
)

sealed interface InteractionSettingsAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetSwipeLeftAction(val action: SwipeActionUiModel) : InteractionSettingsAction
    data class SetSwipeRightAction(val action: SwipeActionUiModel) : InteractionSettingsAction
}

sealed interface InteractionSettingsEffect {
    data object Saved : InteractionSettingsEffect
    data class SaveFailed(val message: String) : InteractionSettingsEffect
}

internal fun InteractionSettingsUiState.toUiModel() = InteractionDetailUiModel(
    isSwipeEnabled = isSwipeEnabled,
    swipeLeftAction = SwipeActionUiModel.valueOf(swipeLeftAction.name),
    swipeRightAction = SwipeActionUiModel.valueOf(swipeRightAction.name),
)

internal fun SwipeActionUiModel.toFeatureModel() = SwipeActionType.valueOf(name)
