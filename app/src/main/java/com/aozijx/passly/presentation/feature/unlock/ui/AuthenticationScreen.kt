package com.aozijx.passly.presentation.feature.unlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.core.ui.components.common.ActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButtonConfig
import com.aozijx.passly.core.ui.components.common.InputActionButtonState
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.unlock.UnlockFailureMessage
import com.aozijx.passly.presentation.feature.unlock.UnlockUiAction
import com.aozijx.passly.presentation.feature.unlock.UnlockUiState
import com.aozijx.passly.presentation.shared.components.apppassword.AppPasswordSetDialog

@Composable
fun AuthenticationScreen(
    state: UnlockUiState,
    onAction: (UnlockUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockIconInteractionSource = remember { MutableInteractionSource() }
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)
    val appPassword = state.appPassword.toUiString()
    val recoveryCode = state.recoveryCode.toUiString()
    val biometricFailureMessage = state.verificationFailure
        ?.takeIf { it.method == AuthenticationMethod.BIOMETRIC }
        ?.message
        ?.localized()
    val appPasswordFailureMessage = state.verificationFailure
        ?.takeIf { it.method == AuthenticationMethod.APP_PASSWORD }
        ?.message
        ?.localized(appPasswordLabel)
    val recoveryCodeFailureMessage = state.verificationFailure
        ?.takeIf { it.method == AuthenticationMethod.RECOVERY_CODE }
        ?.message
        ?.localized(recoveryCodeLabel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clickable(
                        interactionSource = lockIconInteractionSource,
                        indication = null,
                        onClick = { onAction(UnlockUiAction.LockIconClicked) },
                    ),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.vault_auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            biometricFailureMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (AuthenticationMethod.BIOMETRIC in state.availableMethods) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Fingerprint,
                    text = stringResource(R.string.auth_biometric_unlock),
                    progress = state.activeMethod == AuthenticationMethod.BIOMETRIC,
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationMethod.BIOMETRIC,
                    onClick = { onAction(UnlockUiAction.BiometricClicked) },
                )
            }

            if (AuthenticationMethod.APP_PASSWORD in state.availableMethods) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state = InputActionButtonState(
                        value = appPassword,
                        expanded = state.expandedMethod == AuthenticationMethod.APP_PASSWORD,
                        progress = state.activeMethod == AuthenticationMethod.APP_PASSWORD,
                        result = appPasswordFailureMessage?.let { false },
                    ),
                    config = InputActionButtonConfig(
                        collapsedText = stringResource(R.string.auth_password_unlock),
                        expandedText = stringResource(R.string.auth_password_verify),
                        inputLabel = appPasswordLabel,
                        errorText = appPasswordFailureMessage
                            ?: stringResource(R.string.auth_error_failed),
                    ),
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationMethod.APP_PASSWORD,
                    onValueChange = { onAction(UnlockUiAction.AppPasswordChanged(it)) },
                    onExpandedChange = {
                        onAction(UnlockUiAction.InputExpanded(AuthenticationMethod.APP_PASSWORD, it))
                    },
                    onAction = { onAction(UnlockUiAction.AppPasswordSubmitted) },
                    onResultConsumed = { onAction(UnlockUiAction.ClearVerificationFailure) },
                )
            }

            if (state.availableMethods.available.isEmpty()) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.auth_set_app_password),
                    progress = state.isSettingAppPassword,
                    enabled = state.activeMethod == null && !state.isSettingAppPassword,
                    onClick = { onAction(UnlockUiAction.SetPasswordClicked) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (AuthenticationMethod.RECOVERY_CODE in state.availableMethods &&
                state.recoveryUnlockVisible
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state = InputActionButtonState(
                        value = recoveryCode,
                        expanded = state.expandedMethod == AuthenticationMethod.RECOVERY_CODE,
                        progress = state.activeMethod == AuthenticationMethod.RECOVERY_CODE,
                        result = recoveryCodeFailureMessage?.let { false },
                    ),
                    config = InputActionButtonConfig(
                        icon = Icons.Default.Restore,
                        collapsedText = stringResource(R.string.restore_access),
                        expandedText = stringResource(R.string.recovery_code_verify),
                        inputLabel = recoveryCodeLabel,
                        errorText = recoveryCodeFailureMessage
                            ?: stringResource(R.string.auth_error_failed),
                    ),
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    onValueChange = { onAction(UnlockUiAction.RecoveryCodeChanged(it)) },
                    onExpandedChange = {
                        onAction(UnlockUiAction.InputExpanded(AuthenticationMethod.RECOVERY_CODE, it))
                    },
                    onAction = { onAction(UnlockUiAction.RecoveryCodeSubmitted) },
                    onResultConsumed = { onAction(UnlockUiAction.ClearVerificationFailure) },
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (state.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = state.newAppPassword.toUiString(),
            confirmPassword = state.confirmAppPassword.toUiString(),
            confirmEnabled = state.canConfirmAppPassword,
            onNewPasswordChange = { onAction(UnlockUiAction.NewAppPasswordChanged(it)) },
            onConfirmPasswordChange = { onAction(UnlockUiAction.ConfirmAppPasswordChanged(it)) },
            onConfirm = { onAction(UnlockUiAction.SetPasswordConfirmed) },
            onDismiss = { onAction(UnlockUiAction.DismissSetPasswordDialog) },
            isBusy = state.isSettingAppPassword,
            errorMessage = state.setupFailure?.localized(),
        )
    }
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
private fun UnlockFailureMessage.localized(
    methodLabel: String? = null,
): String = when (this) {
    is UnlockFailureMessage.IncorrectCredential -> {
        if (methodLabel != null && (remainingAttempts ?: 0) > 0) {
            stringResource(
                R.string.auth_error_method_incorrect_attempts,
                methodLabel,
                remainingAttempts ?: 0,
            )
        } else if (methodLabel != null) {
            stringResource(R.string.auth_error_method_incorrect, methodLabel)
        } else {
            stringResource(R.string.auth_error_failed)
        }
    }
    is UnlockFailureMessage.RateLimited -> stringResource(
        R.string.auth_error_rate_limited,
        retryAfterSeconds,
    )
    UnlockFailureMessage.PasswordTooShort -> stringResource(
        R.string.auth_error_password_too_short,
    )
    UnlockFailureMessage.AppPasswordSetupFailed -> stringResource(
        R.string.auth_error_app_password_setup_failed,
    )
    UnlockFailureMessage.Generic -> stringResource(R.string.auth_error_failed)
}
