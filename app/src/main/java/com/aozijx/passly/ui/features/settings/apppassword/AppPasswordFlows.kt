package com.aozijx.passly.ui.features.settings.apppassword

import android.content.Context
import android.widget.Toast
import com.aozijx.passly.R
import com.aozijx.passly.core.auth.VerificationGateway
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.ui.components.toUiMessage

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
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_password_mismatch),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            authGateway.setAppPassword(newPassword.toCharArray()) { result ->
                result.onSuccess {
                    Toast.makeText(
                        context,
                        context.getString(R.string.auth_password_set_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_password_fields_required),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_password_mismatch),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            authGateway.changeAppPassword(
                currentPassword.toCharArray(), newPassword.toCharArray()
            ) { result ->
                result.onSuccess {
                    Toast.makeText(
                        context,
                        context.getString(R.string.auth_password_change_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        AppPasswordAction.DISABLE -> {
            if (currentPassword.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_current_password_required),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            authGateway.disableAppPassword(currentPassword.toCharArray()) { result ->
                result.onSuccess {
                    Toast.makeText(
                        context,
                        context.getString(R.string.auth_password_disabled),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
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
    authFailedMsg: String,
    onAlreadyEnabled: () -> Unit,
    onVerified: () -> Unit
) {
    if (isAppPasswordEnabled) {
        onAlreadyEnabled()
        return
    }
    if (launcher == null) {
        Toast.makeText(context, context.getString(R.string.auth_unavailable), Toast.LENGTH_SHORT)
            .show()
        return
    }
    authGateway.verifyWithBiometric(launcher, title, subtitle) { result ->
        result.onSuccess { onVerified() }
            .onFailure { error ->
                val msg = error.toUiMessage(authFailedMsg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }
}