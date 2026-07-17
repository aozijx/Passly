package com.aozijx.passly.feature.settings.apppassword

import android.content.Context
import com.aozijx.passly.R
import com.aozijx.passly.feature.auth.VerificationGateway
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.message.AppMessageCenter

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
    authGateway: VerificationGateway,
    onSuccess: (AppPasswordAction) -> Unit
) {
    when (action) {
        AppPasswordAction.SET -> {
            if (newPassword != confirmPassword) {
                AppMessageCenter.publish(context.getString(R.string.auth_password_mismatch))
                return
            }
            authGateway.setAppPassword(newPassword.toCharArray()) { result ->
                result.onSuccess {
                    AppMessageCenter.publish(context.getString(R.string.auth_password_set_success))
                    onSuccess(action)
                }
            }
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                AppMessageCenter.publish(context.getString(R.string.auth_password_fields_required))
                return
            }
            if (newPassword != confirmPassword) {
                AppMessageCenter.publish(context.getString(R.string.auth_password_mismatch))
                return
            }
            authGateway.changeAppPassword(
                currentPassword.toCharArray(), newPassword.toCharArray()
            ) { result ->
                result.onSuccess {
                    AppMessageCenter.publish(context.getString(R.string.auth_password_change_success))
                    onSuccess(action)
                }
            }
        }

        AppPasswordAction.DISABLE -> {
            if (currentPassword.isEmpty()) {
                AppMessageCenter.publish(context.getString(R.string.auth_current_password_required))
                return
            }
            authGateway.disableAppPassword(currentPassword.toCharArray()) { result ->
                result.onSuccess {
                    AppMessageCenter.publish(context.getString(R.string.auth_password_disabled))
                    onSuccess(action)
                }
            }
        }
    }
}

internal fun handleAppPasswordEntryClick(
    context: Context,
    launcher: BiometricPromptLauncher?,
    isAppPasswordEnabled: Boolean,
    authGateway: VerificationGateway,
    title: String,
    subtitle: String,
    onAlreadyEnabled: () -> Unit,
    onVerified: () -> Unit
) {
    if (isAppPasswordEnabled) {
        onAlreadyEnabled()
        return
    }
    if (authGateway.isAuthorized.value) {
        onVerified()
        return
    }
    if (launcher == null) {
        AppMessageCenter.publish(context.getString(R.string.auth_unavailable))
        return
    }
    authGateway.verifyWithBiometric(launcher, title, subtitle) { result ->
        result.onSuccess { onVerified() }
    }
}
