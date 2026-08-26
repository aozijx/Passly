package com.aozijx.passly.presentation.feature.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.core.ui.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiAction
import com.aozijx.passly.presentation.feature.onboarding.BootstrapViewModel
import com.aozijx.passly.presentation.ui.unlock.AuthenticationContent
import com.aozijx.passly.presentation.ui.unlock.AuthenticationContentState
import com.aozijx.passly.presentation.ui.unlock.AuthenticationEventHandler
import com.aozijx.passly.presentation.ui.unlock.AuthenticationInputMethod

@Composable
fun AuthenticationScreen(
    unlockViewModel: UnlockViewModel,
    bootstrapViewModel: BootstrapViewModel,
) {
    val methods by unlockViewModel.methodAvailability.collectAsStateWithLifecycle()
    val unlockState by unlockViewModel.uiState.collectAsStateWithLifecycle()
    val bootstrapState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
    val verificationFailure = unlockState.verificationFailure
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)

    BackHandler(enabled = !bootstrapState.showSetPasswordDialog) {
        unlockViewModel.onAction(UnlockUiAction.BackPressed)
    }

    AuthenticationContent(
        state = AuthenticationContentState(
            biometricAvailable = AuthenticationMethod.BIOMETRIC in methods,
            appPasswordAvailable = AuthenticationMethod.APP_PASSWORD in methods,
            recoveryCodeAvailable = AuthenticationMethod.RECOVERY_CODE in methods,
            recoveryUnlockVisible = unlockState.recoveryUnlockVisible,
            activeMethod = unlockState.activeMethod?.toUiMethod(),
            expandedMethod = unlockState.expandedMethod?.toUiMethod(),
            appPassword = unlockState.appPassword.toUiString(),
            recoveryCode = unlockState.recoveryCode.toUiString(),
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
            isSettingAppPassword = bootstrapState.isSettingAppPassword,
        ),
        onEvent = AuthenticationEventHandler(
            onLockIconClick = {
                unlockViewModel.onAction(UnlockUiAction.LockIconClicked)
            },
            onBiometricClick = {
                unlockViewModel.onAction(UnlockUiAction.BiometricClicked)
            },
            onAppPasswordChange = {
                unlockViewModel.onAction(UnlockUiAction.AppPasswordChanged(it))
            },
            onAppPasswordExpandedChange = {
                unlockViewModel.onAction(
                    UnlockUiAction.InputExpanded(AuthenticationMethod.APP_PASSWORD, it)
                )
            },
            onAppPasswordSubmit = {
                unlockViewModel.onAction(UnlockUiAction.AppPasswordSubmitted)
            },
            onRecoveryCodeChange = {
                unlockViewModel.onAction(UnlockUiAction.RecoveryCodeChanged(it))
            },
            onRecoveryCodeExpandedChange = {
                unlockViewModel.onAction(
                    UnlockUiAction.InputExpanded(AuthenticationMethod.RECOVERY_CODE, it)
                )
            },
            onRecoveryCodeSubmit = {
                unlockViewModel.onAction(UnlockUiAction.RecoveryCodeSubmitted)
            },
            onVerificationFailureConsumed = {
                unlockViewModel.onAction(UnlockUiAction.ClearVerificationFailure)
            },
            onSetPasswordClick = {
                bootstrapViewModel.onAction(BootstrapUiAction.SetPasswordClicked)
            },
        ),
    )

    if (bootstrapState.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = bootstrapState.newAppPassword.toUiString(),
            confirmPassword = bootstrapState.confirmAppPassword.toUiString(),
            onNewPasswordChange = {
                bootstrapViewModel.onAction(BootstrapUiAction.NewAppPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                bootstrapViewModel.onAction(BootstrapUiAction.ConfirmAppPasswordChanged(it))
            },
            onConfirm = {
                bootstrapViewModel.onAction(BootstrapUiAction.SetPasswordConfirmed)
            },
            onDismiss = {
                bootstrapViewModel.onAction(BootstrapUiAction.DismissSetPasswordDialog)
            },
            isBusy = bootstrapState.isSettingAppPassword,
            errorMessage = bootstrapState.setupFailure?.message(forSetup = true),
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
