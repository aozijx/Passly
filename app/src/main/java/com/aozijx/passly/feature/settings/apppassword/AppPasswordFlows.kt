package com.aozijx.passly.feature.settings.apppassword

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.contract.SettingsIntent

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
            if (!AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length)) {
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
                    newPassword.toCharArray(),
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
