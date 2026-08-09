package com.aozijx.passly.feature.settings.apppassword

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
import com.aozijx.passly.domain.authentication.AppPasswordPolicy
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.contract.SettingsIntent

enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

internal fun validateAndSendAppPasswordAction(
    context: Context,
    action: AppPasswordAction,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    settingsViewModel: SettingsViewModel
): Boolean {
    when (action) {
        AppPasswordAction.SET -> {
            if (!AppPasswordPolicy.acceptsLength(newPassword.length)) {
                context.showToast(R.string.settings_auth_password_too_short)
                return false
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return false
            }
            settingsViewModel.handleIntent(
                SettingsIntent.SetAppPassword(newPassword.toCharArray())
            )
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                context.showToast(R.string.settings_auth_password_fields_required)
                return false
            }
            if (!AppPasswordPolicy.acceptsLength(newPassword.length)) {
                context.showToast(R.string.settings_auth_password_too_short)
                return false
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return false
            }
            settingsViewModel.handleIntent(
                SettingsIntent.ChangeAppPassword(
                    currentPassword.toCharArray(),
                    newPassword.toCharArray()
                )
            )
        }

        AppPasswordAction.DISABLE -> {
            settingsViewModel.handleIntent(SettingsIntent.DisableAppPassword)
        }
    }
    return true
}

private fun Context.showToast(messageResource: Int) {
    Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show()
}

private fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

private fun Context.showFailure(result: AuthenticationResult) {
    if (result is AuthenticationResult.Cancelled) return
    val failure = (result as? AuthenticationResult.Failure)?.failure
    when (failure?.authCode) {
        AuthenticationFailureCode.CREDENTIAL_INCORRECT -> {
            if (failure.remainingAttempts > 0) {
                showToast(
                    getString(
                        R.string.auth_error_method_incorrect_attempts,
                        getString(R.string.auth_app_password_label),
                        failure.remainingAttempts
                    )
                )
            } else {
                showToast(R.string.settings_auth_current_password_incorrect)
            }
        }

        AuthenticationFailureCode.RATE_LIMITED -> showToast(
            getString(
                R.string.auth_error_rate_limited,
                ((failure.retryAfterMs + 999L) / 1000L).coerceAtLeast(1L)
            )
        )

        AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION ->
            showToast(R.string.settings_auth_password_too_short)

        AuthenticationFailureCode.LAST_METHOD_REQUIRED ->
            showToast(R.string.settings_auth_last_method_required)

        else -> showToast(R.string.settings_auth_operation_failed)
    }
}

internal fun handleAppPasswordEntryClick(
    context: Context,
    isAppPasswordEnabled: Boolean,
    settingsViewModel: SettingsViewModel,
    onAlreadyEnabled: () -> Unit,
    onVerified: () -> Unit
) {
    settingsViewModel.authenticationManager.authenticate(
        AuthenticationRequest(AuthenticationPurpose.REAUTHENTICATE)
    ) { result ->
        when (result) {
            is AuthenticationResult.Success -> {
                if (isAppPasswordEnabled) {
                    onAlreadyEnabled()
                } else {
                    onVerified()
                }
            }

            is AuthenticationResult.Cancelled -> Unit
            is AuthenticationResult.Failure -> context.showFailure(result)
        }
    }
}
