package com.aozijx.passly.presentation.feature.settings.security

sealed interface AppPasswordSettingsAction {
    data object RequestAppPasswordEntry : AppPasswordSettingsAction
    data class SetAppPassword(val password: CharArray) : AppPasswordSettingsAction
    data class ChangeAppPassword(
        val currentPassword: CharArray,
        val newPassword: CharArray,
    ) : AppPasswordSettingsAction

    data object DisableAppPassword : AppPasswordSettingsAction
}
