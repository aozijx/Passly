package com.aozijx.passly.presentation.feature.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.presentation.ui.shared.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.unlock.ui.AuthenticationScreen
import com.aozijx.passly.presentation.feature.unlock.ui.AuthenticationScreenState
import com.aozijx.passly.presentation.feature.unlock.ui.AuthenticationScreenEvents
import com.aozijx.passly.presentation.feature.unlock.ui.AuthenticationInputMethod

@Composable
fun AuthenticationRoute(viewModel: UnlockViewModel) {
    val methods by viewModel.methodAvailability.collectAsStateWithLifecycle()
    val unlockState by viewModel.uiState.collectAsStateWithLifecycle()
    val verificationFailure = unlockState.verificationFailure
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)
    val appPassword = unlockState.appPassword.toUiString()
    val recoveryCode = unlockState.recoveryCode.toUiString()

    BackHandler(enabled = !unlockState.showSetPasswordDialog) {
        viewModel.onAction(UnlockUiAction.BackPressed)
    }

    AuthenticationScreen(
        state = AuthenticationScreenState(
            biometricAvailable = AuthenticationMethod.BIOMETRIC in methods,
            appPasswordAvailable = AuthenticationMethod.APP_PASSWORD in methods,
            recoveryCodeAvailable = AuthenticationMethod.RECOVERY_CODE in methods,
            recoveryUnlockVisible = unlockState.recoveryUnlockVisible,
            activeMethod = unlockState.activeMethod?.toUiMethod(),
            expandedMethod = unlockState.expandedMethod?.toUiMethod(),
            appPassword = appPassword,
            recoveryCode = recoveryCode,
            biometricFailureMessage = verificationFailure
                ?.takeIf { it.method == AuthenticationMethod.BIOMETRIC }
                ?.failure
                ?.message(),
            appPasswordFailureMessage = verificationFailure
                ?.takeIf { it.method == AuthenticationMethod.APP_PASSWORD }
                ?.failure
                ?.message(appPasswordLabel),
            recoveryCodeFailureMessage = verificationFailure
                ?.takeIf { it.method == AuthenticationMethod.RECOVERY_CODE }
                ?.failure
                ?.message(recoveryCodeLabel),
            isSettingAppPassword = unlockState.isSettingAppPassword,
        ),
        onEvent = AuthenticationScreenEvents(
            onLockIconClick = {
                viewModel.onAction(UnlockUiAction.LockIconClicked)
            },
            onBiometricClick = {
                viewModel.onAction(UnlockUiAction.BiometricClicked)
            },
            onAppPasswordChange = {
                viewModel.onAction(UnlockUiAction.AppPasswordChanged(it))
            },
            onAppPasswordExpandedChange = {
                viewModel.onAction(
                    UnlockUiAction.InputExpanded(AuthenticationMethod.APP_PASSWORD, it)
                )
            },
            onAppPasswordSubmit = {
                viewModel.onAction(UnlockUiAction.AppPasswordSubmitted)
            },
            onRecoveryCodeChange = {
                viewModel.onAction(UnlockUiAction.RecoveryCodeChanged(it))
            },
            onRecoveryCodeExpandedChange = {
                viewModel.onAction(
                    UnlockUiAction.InputExpanded(AuthenticationMethod.RECOVERY_CODE, it)
                )
            },
            onRecoveryCodeSubmit = {
                viewModel.onAction(UnlockUiAction.RecoveryCodeSubmitted)
            },
            onVerificationFailureConsumed = {
                viewModel.onAction(UnlockUiAction.ClearVerificationFailure)
            },
            onSetPasswordClick = {
                viewModel.onAction(UnlockUiAction.SetPasswordClicked)
            },
        ),
    )

    if (unlockState.showSetPasswordDialog) {
        val newPassword = unlockState.newAppPassword.toUiString()
        val confirmPassword = unlockState.confirmAppPassword.toUiString()
        AppPasswordSetDialog(
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            confirmEnabled = AppPasswordPolicy.DEFAULT.acceptsLength(
                newPassword.length,
            ) && newPassword == confirmPassword,
            onNewPasswordChange = {
                viewModel.onAction(UnlockUiAction.NewAppPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged(it))
            },
            onConfirm = {
                viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            },
            onDismiss = {
                viewModel.onAction(UnlockUiAction.DismissSetPasswordDialog)
            },
            isBusy = unlockState.isSettingAppPassword,
            errorMessage = unlockState.setupFailure?.message(forSetup = true),
        )
    }
}

private fun AuthenticationMethod.toUiMethod(): AuthenticationInputMethod = when (this) {
    AuthenticationMethod.BIOMETRIC -> AuthenticationInputMethod.BIOMETRIC
    AuthenticationMethod.APP_PASSWORD -> AuthenticationInputMethod.APP_PASSWORD
    AuthenticationMethod.RECOVERY_CODE -> AuthenticationInputMethod.RECOVERY_CODE
}

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        MemoryCleaner.wipeCharArray(chars)
    }
}

@Composable
private fun AuthenticationFailure.message(
    methodLabel: String? = null,
    forSetup: Boolean = false,
): String {
    if (code == AuthenticationFailureCode.CREDENTIAL_INCORRECT && methodLabel != null) {
        if ((attempts.remaining ?: 0) > 0) {
            return stringResource(
                R.string.auth_error_method_incorrect_attempts,
                methodLabel,
                attempts.remaining ?: 0,
            )
        }
        return stringResource(R.string.auth_error_method_incorrect, methodLabel)
    }

    val message = when {
        forSetup && code == AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION ->
            R.string.auth_error_password_too_short

        code == AuthenticationFailureCode.RATE_LIMITED -> R.string.auth_error_rate_limited
        forSetup -> R.string.auth_error_app_password_setup_failed
        else -> R.string.auth_error_failed
    }
    return if (code == AuthenticationFailureCode.RATE_LIMITED) {
        stringResource(message, (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L))
    } else {
        stringResource(message)
    }
}
