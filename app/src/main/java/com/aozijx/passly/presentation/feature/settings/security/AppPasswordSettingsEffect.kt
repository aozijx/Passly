package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationFailure

sealed interface AppPasswordSettingsEffect {
    data object AppPasswordSet : AppPasswordSettingsEffect
    data object AppPasswordChanged : AppPasswordSettingsEffect
    data object AppPasswordDisabled : AppPasswordSettingsEffect
    data class AppPasswordError(val message: String) : AppPasswordSettingsEffect
    data class AppPasswordEntryAuthorized(val alreadyEnabled: Boolean) : AppPasswordSettingsEffect
    data class AppPasswordEntryAuthenticationFailed(
        val failure: AuthenticationFailure,
    ) : AppPasswordSettingsEffect
}
