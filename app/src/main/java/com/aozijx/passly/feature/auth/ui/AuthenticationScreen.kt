package com.aozijx.passly.feature.auth.ui

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
import com.aozijx.passly.feature.auth.contract.AuthenticationIntent
import com.aozijx.passly.feature.auth.presentation.AuthenticationViewModel

@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel
) {
    val methods by viewModel.methodAvailability.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val activeMethod = uiState.activeMethod
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.auth_recovery_code_label)
    val verificationFailure = uiState.verificationFailure
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

    BackHandler(enabled = !uiState.showSetPasswordDialog) {
        viewModel.onIntent(AuthenticationIntent.BackPressed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clickable(
                        interactionSource = lockIconInteractionSource,
                        indication = null
                    ) {
                        viewModel.onIntent(AuthenticationIntent.LockIconClicked)
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
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.BIOMETRIC,
                    onClick = { viewModel.onIntent(AuthenticationIntent.BiometricClicked) }
                )
            }

            if (AuthenticationMethod.APP_PASSWORD in methods) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    value = uiState.appPassword,
                    expanded = uiState.expandedMethod == AuthenticationMethod.APP_PASSWORD,
                    progress = activeMethod == AuthenticationMethod.APP_PASSWORD,
                    collapsedText = stringResource(R.string.auth_password_unlock),
                    expandedText = stringResource(R.string.auth_password_verify),
                    inputLabel = appPasswordLabel,
                    result = appPasswordFailure?.let { false },
                    errorText = appPasswordFailure?.message(appPasswordLabel)
                        ?: stringResource(R.string.auth_error_failed),
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.APP_PASSWORD,
                    onValueChange = {
                        viewModel.onIntent(AuthenticationIntent.AppPasswordChanged(it))
                    },
                    onExpandedChange = {
                        viewModel.onIntent(
                            AuthenticationIntent.InputExpanded(
                                AuthenticationMethod.APP_PASSWORD,
                                it
                            )
                        )
                    },
                    onAction = { viewModel.onIntent(AuthenticationIntent.AppPasswordSubmitted) },
                    onResultConsumed = {
                        viewModel.onIntent(AuthenticationIntent.ClearVerificationFailure)
                    }
                )
            }

            if (methods.available.isEmpty()) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.auth_set_app_password),
                    progress = uiState.isSettingAppPassword,
                    enabled = activeMethod == null && !uiState.isSettingAppPassword,
                    onClick = { viewModel.onIntent(AuthenticationIntent.SetPasswordClicked) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (AuthenticationMethod.RECOVERY_CODE in methods && uiState.recoveryUnlockVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    value = uiState.recoveryCode,
                    expanded = uiState.expandedMethod == AuthenticationMethod.RECOVERY_CODE,
                    progress = activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    icon = Icons.Default.Restore,
                    collapsedText = stringResource(R.string.auth_use_recovery_code),
                    expandedText = stringResource(R.string.auth_recovery_code_unlock),
                    inputLabel = recoveryCodeLabel,
                    result = recoveryCodeFailure?.let { false },
                    errorText = recoveryCodeFailure?.message(recoveryCodeLabel)
                        ?: stringResource(R.string.auth_error_failed),
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    onValueChange = {
                        viewModel.onIntent(AuthenticationIntent.RecoveryCodeChanged(it))
                    },
                    onExpandedChange = {
                        viewModel.onIntent(
                            AuthenticationIntent.InputExpanded(
                                AuthenticationMethod.RECOVERY_CODE, it
                            )
                        )
                    },
                    onAction = { viewModel.onIntent(AuthenticationIntent.RecoveryCodeSubmitted) },
                    onResultConsumed = {
                        viewModel.onIntent(AuthenticationIntent.ClearVerificationFailure)
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (uiState.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = String(uiState.newAppPassword.toCharArray()),
            confirmPassword = String(uiState.confirmAppPassword.toCharArray()),
            onNewPasswordChange = {
                viewModel.onIntent(AuthenticationIntent.NewAppPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                viewModel.onIntent(AuthenticationIntent.ConfirmAppPasswordChanged(it))
            },
            onConfirm = { viewModel.onIntent(AuthenticationIntent.SetPasswordConfirmed) },
            onDismiss = { viewModel.onIntent(AuthenticationIntent.DismissSetPasswordDialog) },
            isBusy = uiState.isSettingAppPassword,
            errorMessage = uiState.setupFailure?.message(forSetup = true)
        )
    }
}

@Composable
private fun AuthenticationFailure.message(
    methodLabel: String? = null,
    forSetup: Boolean = false
): String {
    if (
        code == AuthenticationFailureCode.CREDENTIAL_INCORRECT &&
        methodLabel != null
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
