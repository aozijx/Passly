package com.aozijx.passly.feature.settings.contract

import com.aozijx.passly.domain.access.model.AuthenticationFailure

sealed interface SettingsEffect {
    data class ShowError(val message: String) : SettingsEffect
    data object SettingsSaved : SettingsEffect
    data object DatabaseCleared : SettingsEffect
    data object AppPasswordSet : SettingsEffect
    data object AppPasswordChanged : SettingsEffect
    data object AppPasswordDisabled : SettingsEffect
    data class AppPasswordError(val message: String) : SettingsEffect
    data class AppPasswordEntryAuthorized(val alreadyEnabled: Boolean) : SettingsEffect
    data class AppPasswordEntryAuthenticationFailed(
        val failure: AuthenticationFailure,
    ) : SettingsEffect
}
