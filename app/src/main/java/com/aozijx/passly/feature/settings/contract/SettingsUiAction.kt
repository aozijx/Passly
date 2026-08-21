package com.aozijx.passly.feature.settings.contract

import com.aozijx.passly.domain.settings.model.SwipeActionType

sealed interface SettingsUiAction {
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsUiAction
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsUiAction
    data object LoadSettings : SettingsUiAction
    data object ClearDatabase : SettingsUiAction
    data object RequestAppPasswordEntry : SettingsUiAction
    data class SetAppPassword(val password: CharArray) : SettingsUiAction
    data class ChangeAppPassword(
        val currentPassword: CharArray,
        val newPassword: CharArray
    ) : SettingsUiAction

    data object DisableAppPassword : SettingsUiAction
}
