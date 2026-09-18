package com.aozijx.passly.presentation.feature.settings.security

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.AppPasswordSettingsAction

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
    settingsViewModel: AppPasswordSettingsViewModel,
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
                AppPasswordSettingsAction.SetAppPassword(newPassword.toCharArray())
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
                AppPasswordSettingsAction.ChangeAppPassword(
                    currentPassword.toCharArray(),
                    newPassword.toCharArray(),
                )
            )
        }

        AppPasswordAction.DISABLE -> {
            settingsViewModel.onAction(AppPasswordSettingsAction.DisableAppPassword)
        }
    }
    return true
}

private fun Context.showToast(messageResource: Int) {
    Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show()
}
