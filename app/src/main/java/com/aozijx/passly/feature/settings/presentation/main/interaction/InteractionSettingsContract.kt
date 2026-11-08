package com.aozijx.passly.feature.settings.presentation.main.interaction

import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface InteractionSettingsAction {
    data object RetryRead : InteractionSettingsAction

    data class SetSwipeEnabled(
        val enabled: Boolean,
    ) : InteractionSettingsAction

    data class SetPullToSearchEnabled(
        val enabled: Boolean,
    ) : InteractionSettingsAction

    data class SetSwipeLeftAction(
        val action: SwipeActionType,
    ) : InteractionSettingsAction

    data class SetSwipeRightAction(
        val action: SwipeActionType,
    ) : InteractionSettingsAction
}

sealed interface InteractionSettingsEffect {
    data object Saved : InteractionSettingsEffect

    data class SaveFailed(
        val message: UiErrorMessage,
    ) : InteractionSettingsEffect
}
