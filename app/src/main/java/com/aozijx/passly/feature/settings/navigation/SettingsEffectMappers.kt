package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.feature.settings.contract.SettingsEffect

internal fun SettingsEffect.toMessage(context: Context): String? = when (this) {
    is SettingsEffect.ShowError -> message
    is SettingsEffect.SettingsSaved -> context.getString(R.string.settings_saved)
    is SettingsEffect.DatabaseCleared -> context.getString(R.string.database_cleared)
    is SettingsEffect.AppPasswordSet -> context.getString(R.string.settings_auth_password_set_success)
    is SettingsEffect.AppPasswordChanged -> context.getString(R.string.settings_auth_password_change_success)
    is SettingsEffect.AppPasswordDisabled -> context.getString(R.string.settings_auth_password_disabled)
    is SettingsEffect.AppPasswordError -> message
    is SettingsEffect.AppPasswordEntryAuthorized -> null
    is SettingsEffect.AppPasswordEntryAuthenticationFailed -> failure.toMessage(context)
}

private fun AuthenticationFailure.toMessage(context: Context): String = when (authCode) {
    AuthenticationFailureCode.CREDENTIAL_INCORRECT -> {
        if (remainingAttempts > 0) {
            context.getString(
                R.string.auth_error_method_incorrect_attempts,
                context.getString(R.string.auth_app_password_label),
                remainingAttempts,
            )
        } else {
            context.getString(R.string.settings_auth_current_password_incorrect)
        }
    }
    AuthenticationFailureCode.RATE_LIMITED -> context.getString(
        R.string.auth_error_rate_limited,
        ((retryAfterMs + 999L) / 1000L).coerceAtLeast(1L),
    )
    AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION ->
        context.getString(R.string.settings_auth_password_too_short)
    AuthenticationFailureCode.LAST_METHOD_REQUIRED ->
        context.getString(R.string.settings_auth_last_method_required)
    else -> context.getString(R.string.settings_auth_operation_failed)
}
