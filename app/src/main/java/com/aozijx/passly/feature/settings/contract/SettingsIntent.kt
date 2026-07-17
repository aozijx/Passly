package com.aozijx.passly.feature.settings.contract

import com.aozijx.passly.domain.model.settings.SwipeActionType

sealed interface SettingsIntent {
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsIntent
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsIntent
    data object LoadSettings : SettingsIntent
}