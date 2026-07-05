package com.aozijx.passly.ui.features.settings.contract

import com.aozijx.passly.domain.model.SwipeActionType

sealed interface SettingsIntent {
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsIntent
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsIntent
    data object LoadSettings : SettingsIntent
}