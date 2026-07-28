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

enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

internal fun handleAppPasswordAction(
    context: Context,
    action: AppPasswordAction,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    settingsViewModel: SettingsViewModel,
    onSuccess: (AppPasswordAction) -> Unit
) {
    when (action) {
        AppPasswordAction.SET -> {
            if (!AppPasswordPolicy.acceptsLength(newPassword.length)) {
                context.showToast(R.string.settings_auth_password_too_short)
                return
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return
            }
            settingsViewModel.setAppPassword(newPassword.toCharArray()) { result ->
                if (result is AuthenticationResult.Success) {
                    context.showToast(R.string.settings_auth_password_set_success)
                    onSuccess(action)
                } else {
                    context.showFailure(result)
                }
            }
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                context.showToast(R.string.settings_auth_password_fields_required)
                return
            }
            if (!AppPasswordPolicy.acceptsLength(newPassword.length)) {
                context.showToast(R.string.settings_auth_password_too_short)
                return
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return
            }
            settingsViewModel.changeAppPassword(
                currentPassword.toCharArray(),
                newPassword.toCharArray()
            ) { result ->
                if (result is AuthenticationResult.Success) {
                    context.showToast(R.string.settings_auth_password_change_success)
                    onSuccess(action)
                } else {
                    context.showFailure(result)
                }
            }
        }

        AppPasswordAction.DISABLE -> {
            settingsViewModel.disableAppPassword { result ->
                if (result is AuthenticationResult.Success) {
                    context.showToast(R.string.settings_auth_password_disabled)
                    onSuccess(action)
                } else {
                    context.showFailure(result)
                }
            }
        }
    }
}

private fun Context.showToast(messageResource: Int) {
    Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show()
}

private fun Context.showFailure(result: AuthenticationResult) {
    if (result is AuthenticationResult.Cancelled) return
    val messageResource = when ((result as? AuthenticationResult.Failure)?.failure?.authCode) {
        AuthenticationFailureCode.CREDENTIAL_INCORRECT ->
            R.string.settings_auth_current_password_incorrect

        AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION ->
            R.string.settings_auth_password_too_short

        AuthenticationFailureCode.LAST_METHOD_REQUIRED ->
            R.string.settings_auth_last_method_required

        else -> R.string.settings_auth_operation_failed
    }
    showToast(messageResource)
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
