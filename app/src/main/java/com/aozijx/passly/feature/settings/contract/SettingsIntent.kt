package com.aozijx.passly.feature.settings.contract

import com.aozijx.passly.domain.settings.model.SwipeActionType

sealed interface SettingsIntent {
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsIntent
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsIntent
    data object LoadSettings : SettingsIntent
    data object ClearDatabase : SettingsIntent
    data class SetAppPassword(val password: CharArray) : SettingsIntent
    data class ChangeAppPassword(
        val currentPassword: CharArray,
        val newPassword: CharArray
    ) : SettingsIntent

    data object DisableAppPassword : SettingsIntent
}
