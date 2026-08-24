package com.aozijx.passly.presentation.feature.settings.security

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction

enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE,
}

internal fun validateAndSendAppPasswordAction(
    context: Context,
    action: AppPasswordAction,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    settingsViewModel: SettingsViewModel,
): Boolean {
    when (action) {
        AppPasswordAction.SET -> {
            if (!AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length)) {
                context.showToast(R.string.auth_error_password_too_short)
                return false
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return false
            }
            settingsViewModel.onAction(
                SettingsUiAction.SetAppPassword(newPassword.toCharArray())
            )
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                context.showToast(R.string.settings_auth_password_fields_required)
                return false
            }
            if (!AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length)) {
                context.showToast(R.string.auth_error_password_too_short)
                return false
            }
            if (newPassword != confirmPassword) {
                context.showToast(R.string.settings_auth_password_mismatch)
                return false
            }
            settingsViewModel.onAction(
                SettingsUiAction.ChangeAppPassword(
                    currentPassword.toCharArray(),
                    newPassword.toCharArray(),
                )
            )
        }

        AppPasswordAction.DISABLE -> {
            settingsViewModel.onAction(SettingsUiAction.DisableAppPassword)
        }
    }
    return true
}

private fun Context.showToast(messageResource: Int) {
    Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show()
}
