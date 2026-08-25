package com.aozijx.passly.presentation.feature.settings.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.ui.settings.interaction.InteractionDetailUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel

data class InteractionSettingsUiState(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
)

sealed interface InteractionSettingsAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionSettingsAction
}

internal fun InteractionSettingsUiState.toUiModel() = InteractionDetailUiModel(
    isSwipeEnabled = isSwipeEnabled,
    swipeLeftAction = VaultSwipeActionUiModel.valueOf(swipeLeftAction.name),
    swipeRightAction = VaultSwipeActionUiModel.valueOf(swipeRightAction.name),
)

internal fun VaultSwipeActionUiModel.toFeatureModel() = SwipeActionType.valueOf(name)
