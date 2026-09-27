package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.feature.settings.security.AppPasswordInputError

sealed interface AppPasswordSettingsEffect {
    data object AppPasswordSet : AppPasswordSettingsEffect
    data object AppPasswordChanged : AppPasswordSettingsEffect
    data object AppPasswordDisabled : AppPasswordSettingsEffect
    data class AppPasswordInputInvalid(
        val reason: AppPasswordInputError,
    ) : AppPasswordSettingsEffect
    data class AppPasswordOperationFailed(
        val failure: AuthenticationFailure,
    ) : AppPasswordSettingsEffect
    data class AppPasswordEntryAuthorized(val alreadyEnabled: Boolean) : AppPasswordSettingsEffect
    data class AppPasswordEntryAuthenticationFailed(
        val failure: AuthenticationFailure,
    ) : AppPasswordSettingsEffect
}
