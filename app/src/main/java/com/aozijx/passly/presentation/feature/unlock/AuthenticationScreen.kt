package com.aozijx.passly.presentation.feature.unlock

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.core.ui.components.common.ActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiAction
import com.aozijx.passly.presentation.feature.unlock.UnlockUiAction
import com.aozijx.passly.presentation.feature.onboarding.BootstrapViewModel
import com.aozijx.passly.presentation.feature.unlock.UnlockViewModel

@Composable
fun AuthenticationScreen(
    unlockViewModel: UnlockViewModel,
    bootstrapViewModel: BootstrapViewModel,
) {
    val methods by unlockViewModel.methodAvailability.collectAsStateWithLifecycle()
    val unlockState by unlockViewModel.uiState.collectAsStateWithLifecycle()
    val bootstrapState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()

    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val activeMethod = unlockState.activeMethod
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)
    val verificationFailure = unlockState.verificationFailure
    val appPasswordFailure = verificationFailure?.takeIf {
        it.method == AuthenticationMethod.APP_PASSWORD
    }?.failure
    val recoveryCodeFailure = verificationFailure?.takeIf {
        it.method == AuthenticationMethod.RECOVERY_CODE
    }?.failure
    val biometricFailure = verificationFailure?.takeIf {
        it.method == AuthenticationMethod.BIOMETRIC
    }?.failure
    val lockIconInteractionSource = remember { MutableInteractionSource() }

    BackHandler(enabled = !bootstrapState.showSetPasswordDialog) {
        unlockViewModel.onAction(UnlockUiAction.BackPressed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                    ) {
                        unlockViewModel.onAction(UnlockUiAction.LockIconClicked)
                    },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (biometricFailure != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = biometricFailure.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (AuthenticationMethod.BIOMETRIC in methods) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Fingerprint,
                    text = stringResource(R.string.auth_biometric_unlock),
                    progress = activeMethod == AuthenticationMethod.BIOMETRIC,
                    enabled = ((activeMethod == null) || (activeMethod == AuthenticationMethod.BIOMETRIC)),
                ) {
                    unlockViewModel.onAction(UnlockUiAction.BiometricClicked)
                }
            }

            if (AuthenticationMethod.APP_PASSWORD in methods) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    value = unlockState.appPassword,
                    expanded = unlockState.expandedMethod == AuthenticationMethod.APP_PASSWORD,
                    progress = activeMethod == AuthenticationMethod.APP_PASSWORD,
                    collapsedText = stringResource(R.string.auth_password_unlock),
                    expandedText = stringResource(R.string.auth_password_verify),
                    inputLabel = appPasswordLabel,
                    result = appPasswordFailure?.let { false },
                    errorText = appPasswordFailure?.message(appPasswordLabel)
                        ?: stringResource(R.string.auth_error_failed),
                    enabled = (activeMethod == null || activeMethod == AuthenticationMethod.APP_PASSWORD),
                    onValueChange = {
                        unlockViewModel.onAction(UnlockUiAction.AppPasswordChanged(it))
                    },
                    onExpandedChange = {
                        unlockViewModel.onAction(
                            UnlockUiAction.InputExpanded(
                                AuthenticationMethod.APP_PASSWORD,
                                it
                            )
                        )
                    },
                    onAction = { unlockViewModel.onAction(UnlockUiAction.AppPasswordSubmitted) },
                    onResultConsumed = {
                        unlockViewModel.onAction(UnlockUiAction.ClearVerificationFailure)
                    }
                )
            }

            if (methods.available.isEmpty()) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.auth_set_app_password),
                    progress = bootstrapState.isSettingAppPassword,
                    enabled = (activeMethod == null && !bootstrapState.isSettingAppPassword),
                ) {
                    bootstrapViewModel.onAction(BootstrapUiAction.SetPasswordClicked)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (AuthenticationMethod.RECOVERY_CODE in methods && unlockState.recoveryUnlockVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    value = unlockState.recoveryCode,
                    expanded = unlockState.expandedMethod == AuthenticationMethod.RECOVERY_CODE,
                    progress = activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    icon = Icons.Default.Restore,
                    collapsedText = stringResource(R.string.restore_access),
                    expandedText = stringResource(R.string.recovery_code_verify),
                    inputLabel = recoveryCodeLabel,
                    result = recoveryCodeFailure?.let { false },
                    errorText = recoveryCodeFailure?.message(recoveryCodeLabel)
                        ?: stringResource(R.string.auth_error_failed),
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    onValueChange = {
                        unlockViewModel.onAction(UnlockUiAction.RecoveryCodeChanged(it))
                    },
                    onExpandedChange = {
                        unlockViewModel.onAction(
                            UnlockUiAction.InputExpanded(
                                AuthenticationMethod.RECOVERY_CODE, it
                            )
                        )
                    },
                    onAction = { unlockViewModel.onAction(UnlockUiAction.RecoveryCodeSubmitted) },
                    onResultConsumed = {
                        unlockViewModel.onAction(UnlockUiAction.ClearVerificationFailure)
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (bootstrapState.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = String(bootstrapState.newAppPassword.toCharArray()),
            confirmPassword = String(bootstrapState.confirmAppPassword.toCharArray()),
            onNewPasswordChange = {
                bootstrapViewModel.onAction(BootstrapUiAction.NewAppPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                bootstrapViewModel.onAction(BootstrapUiAction.ConfirmAppPasswordChanged(it))
            },
            onConfirm = { bootstrapViewModel.onAction(BootstrapUiAction.SetPasswordConfirmed) },
            onDismiss = { bootstrapViewModel.onAction(BootstrapUiAction.DismissSetPasswordDialog) },
            isBusy = bootstrapState.isSettingAppPassword,
            errorMessage = bootstrapState.setupFailure?.message(forSetup = true)
        )
    }
}

@Composable
private fun AuthenticationFailure.message(
    methodLabel: String? = null,
    forSetup: Boolean = false
): String {
    if (
        (code == AuthenticationFailureCode.CREDENTIAL_INCORRECT) &&
        (methodLabel != null)
    ) {
        if ((attempts.remaining ?: 0) > 0) {
            return stringResource(
                R.string.auth_error_method_incorrect_attempts,
                methodLabel,
                attempts.remaining ?: 0
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
