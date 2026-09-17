package com.aozijx.passly.presentation.feature.settings.main

sealed interface SettingsUiAction {
    data object RequestAppPasswordEntry : SettingsUiAction
    data class SetAppPassword(val password: CharArray) : SettingsUiAction
    data class ChangeAppPassword(
        val currentPassword: CharArray,
        val newPassword: CharArray,
    ) : SettingsUiAction

    data object DisableAppPassword : SettingsUiAction
}
