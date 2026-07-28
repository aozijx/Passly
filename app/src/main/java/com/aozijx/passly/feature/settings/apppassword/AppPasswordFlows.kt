package com.aozijx.passly.feature.settings.apppassword

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
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
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return
            }
            settingsViewModel.setAppPassword(newPassword.toCharArray()) { success ->
                if (success) {
                    context.showToast(R.string.settings_auth_password_set_success)
                    onSuccess(action)
                }
            }
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                context.showToast(R.string.settings_auth_password_fields_required)
                return
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return
            }
            settingsViewModel.changeAppPassword(newPassword.toCharArray()) { success ->
                if (success) {
                    context.showToast(R.string.settings_auth_password_change_success)
                    onSuccess(action)
                }
            }
        }

        AppPasswordAction.DISABLE -> {
            settingsViewModel.disableAppPassword { success ->
                if (success) {
                    context.showToast(R.string.settings_auth_password_disabled)
                    onSuccess(action)
                }
            }
        }
    }
}

private fun Context.showToast(messageResource: Int) {
    Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show()
}

internal fun handleAppPasswordEntryClick(
    context: Context,
    isAppPasswordEnabled: Boolean,
    settingsViewModel: SettingsViewModel,
    title: String,
    subtitle: String,
    onAlreadyEnabled: () -> Unit,
    onVerified: () -> Unit
) {
    settingsViewModel.authenticationManager.authenticate(
        AuthenticationRequest(AuthenticationPurpose.REAUTHENTICATE)
    ) { result ->
        if (result is AuthenticationResult.Success) {
            if (isAppPasswordEnabled) {
                onAlreadyEnabled()
            } else {
                onVerified()
            }
        }
    }
}
