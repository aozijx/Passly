package com.aozijx.passly.feature.unlock.presentation.ui

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
import com.aozijx.passly.core.ui.components.common.ActionButtonPhase
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButtonConfig
import com.aozijx.passly.core.ui.components.common.InputActionButtonState
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.unlock.presentation.KdfInitializationUiState
import com.aozijx.passly.feature.unlock.presentation.PasswordSetupFeedback
import com.aozijx.passly.feature.unlock.presentation.UnlockFailureMessage
import com.aozijx.passly.feature.unlock.presentation.UnlockPasswordSetup
import com.aozijx.passly.feature.unlock.presentation.UnlockUiAction
import com.aozijx.passly.feature.unlock.presentation.UnlockUiState
import com.aozijx.passly.feature.unlock.presentation.UnlockVerificationPhase
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import com.aozijx.passly.presentation.mapping.toUiState
import com.aozijx.passly.presentation.shared.components.apppassword.AppPasswordSetDialog

@Composable
fun AuthenticationScreen(
    state: UnlockUiState,
    onAction: (UnlockUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockIconInteractionSource = remember { MutableInteractionSource() }
    val appPasswordLabel = stringResource(R.string.authentication_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)
    val appPassword = state.appPassword.toUiString()
    val recoveryCode = state.recoveryCode.toUiString()
    val activeMethod = (state.verificationPhase as? UnlockVerificationPhase.Authenticating)?.method
    val verificationFailure = (state.verificationPhase as? UnlockVerificationPhase.Failed)?.failure
    val appPasswordAttemptStatus = state.authenticationAttemptStatuses[AuthenticationMethod.APP_PASSWORD]
    val recoveryCodeAttemptStatus = state.authenticationAttemptStatuses[AuthenticationMethod.RECOVERY_CODE]
    val biometricFailureMessage =
        verificationFailure
            ?.takeIf { it.method == AuthenticationMethod.BIOMETRIC }
            ?.message
            ?.localized()
    val appPasswordFailureMessage =
        verificationFailure
            ?.takeIf { it.method == AuthenticationMethod.APP_PASSWORD }
            ?.message
            ?.localized(appPasswordLabel)
    val recoveryCodeFailureMessage =
        verificationFailure
            ?.takeIf { it.method == AuthenticationMethod.RECOVERY_CODE }
            ?.message
            ?.localized(recoveryCodeLabel)

    Column(
        modifier =
            modifier
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
                modifier =
                    Modifier
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
                text = stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.unlock_subtitle),
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
                    text = stringResource(R.string.unlock_biometric),
                    phase =
                        if (activeMethod == AuthenticationMethod.BIOMETRIC) {
                            ActionButtonPhase.InProgress
                        } else {
                            ActionButtonPhase.Idle
                        },
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.BIOMETRIC,
                    onClick = { onAction(UnlockUiAction.BiometricClicked) },
                )
            }

            if (AuthenticationMethod.APP_PASSWORD in state.availableMethods) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state =
                        InputActionButtonState(
                            value = appPassword,
                            canSubmit = state.canSubmitAppPassword,
                            expanded = state.inputExpansion == AuthenticationMethod.APP_PASSWORD,
                            phase =
                                when {
                                    activeMethod == AuthenticationMethod.APP_PASSWORD -> {
                                        ActionButtonPhase.InProgress
                                    }

                                    appPasswordFailureMessage != null -> {
                                        ActionButtonPhase.Failed
                                    }

                                    else -> {
                                        ActionButtonPhase.Idle
                                    }
                                },
                        ),
                    config =
                        InputActionButtonConfig(
                            collapsedText = stringResource(R.string.unlock_with_password),
                            expandedText = stringResource(R.string.unlock_password_verify),
                            inputLabel = appPasswordLabel,
                            errorText =
                                appPasswordFailureMessage
                                    ?: stringResource(R.string.authentication_error_failed),
                        ),
                    enabled =
                        (activeMethod == null || activeMethod == AuthenticationMethod.APP_PASSWORD) &&
                            appPasswordAttemptStatus?.retryAfterMs == null,
                    onValueChange = { onAction(UnlockUiAction.AppPasswordChanged(it)) },
                    onExpandedChange = {
                        onAction(UnlockUiAction.InputExpanded(AuthenticationMethod.APP_PASSWORD, it))
                    },
                    onAction = { onAction(UnlockUiAction.AppPasswordSubmitted) },
                    onResultConsumed = { onAction(UnlockUiAction.ClearVerificationFailure) },
                )
            }

            if (state.shouldOfferPasswordSetup) {
                val isSaving = state.passwordSetup == UnlockPasswordSetup.Saving
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.authentication_set_app_password),
                    phase = if (isSaving) ActionButtonPhase.InProgress else ActionButtonPhase.Idle,
                    enabled = activeMethod == null && !isSaving,
                    onClick = { onAction(UnlockUiAction.SetPasswordClicked) },
                )
            }

            if (AuthenticationMethod.RECOVERY_CODE in state.availableMethods &&
                state.recoveryUnlockVisible
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state =
                        InputActionButtonState(
                            value = recoveryCode,
                            canSubmit = state.canSubmitRecoveryCode,
                            expanded = state.inputExpansion == AuthenticationMethod.RECOVERY_CODE,
                            phase =
                                when {
                                    activeMethod == AuthenticationMethod.RECOVERY_CODE -> {
                                        ActionButtonPhase.InProgress
                                    }

                                    recoveryCodeFailureMessage != null -> {
                                        ActionButtonPhase.Failed
                                    }

                                    else -> {
                                        ActionButtonPhase.Idle
                                    }
                                },
                        ),
                    config =
                        InputActionButtonConfig(
                            icon = Icons.Default.Restore,
                            collapsedText = stringResource(R.string.restore_access),
                            expandedText = stringResource(R.string.authentication_verify_recovery_code),
                            inputLabel = recoveryCodeLabel,
                            errorText =
                                recoveryCodeFailureMessage
                                    ?: stringResource(R.string.authentication_error_failed),
                        ),
                    enabled =
                        (activeMethod == null || activeMethod == AuthenticationMethod.RECOVERY_CODE) &&
                            recoveryCodeAttemptStatus?.retryAfterMs == null,
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

    val passwordSetup = state.passwordSetup
    val passwordDraft =
        when (passwordSetup) {
            UnlockPasswordSetup.Closed -> null
            UnlockPasswordSetup.Saving -> PasswordSetupDraft()
            is UnlockPasswordSetup.Editing -> passwordSetup.draft
        }
    val passwordSetupCanSubmit =
        when (passwordSetup) {
            UnlockPasswordSetup.Closed,
            UnlockPasswordSetup.Saving,
            -> false

            is UnlockPasswordSetup.Editing -> passwordSetup.canSubmit
        }
    val passwordSetupErrorMessage =
        when (passwordSetup) {
            UnlockPasswordSetup.Closed, UnlockPasswordSetup.Saving -> {
                null
            }

            is UnlockPasswordSetup.Editing -> {
                when (val feedback = passwordSetup.feedback) {
                    PasswordSetupFeedback.None -> null
                    is PasswordSetupFeedback.Failed -> feedback.message.localized()
                }
            }
        }
    if (passwordDraft != null) {
        AppPasswordSetDialog(
            newPassword = passwordDraft.newPassword.toUiString(),
            confirmPassword = passwordDraft.confirmPassword.toUiString(),
            strengthUiState =
                passwordDraft.passwordStrength.toUiState(MaterialTheme.colorScheme),
            confirmationState = passwordDraft.confirmationState,
            confirmEnabled =
                passwordSetupCanSubmit &&
                    state.kdfInitialization is KdfInitializationUiState.Ready,
            kdfHint =
                when (val kdfInitialization = state.kdfInitialization) {
                    KdfInitializationUiState.Failed -> {
                        stringResource(R.string.authentication_kdf_initialization_failed)
                    }

                    KdfInitializationUiState.Loading -> {
                        stringResource(R.string.authentication_kdf_calibrating)
                    }

                    is KdfInitializationUiState.Ready -> {
                        stringResource(
                            R.string.authentication_kdf_selected,
                            kdfInitialization.parameters.memoryKiB / 1024,
                            kdfInitialization.parameters.iterations,
                            kdfInitialization.parameters.parallelism,
                        )
                    }
                },
            isKdfLoading = state.kdfInitialization is KdfInitializationUiState.Loading,
            onNewPasswordChange = { onAction(UnlockUiAction.NewAppPasswordChanged(it)) },
            onConfirmPasswordChange = { onAction(UnlockUiAction.ConfirmAppPasswordChanged(it)) },
            onConfirm = { onAction(UnlockUiAction.SetPasswordConfirmed) },
            onDismiss = { onAction(UnlockUiAction.DismissSetPasswordDialog) },
            isBusy = passwordSetup == UnlockPasswordSetup.Saving,
            errorMessage = passwordSetupErrorMessage,
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
private fun UnlockFailureMessage.localized(methodLabel: String? = null): String =
    when (this) {
        is UnlockFailureMessage.IncorrectCredential -> {
            if ((remainingAttempts ?: 0) > 0) {
                stringResource(
                    R.string.authentication_error_credential_incorrect_attempts,
                    methodLabel.orEmpty(),
                    remainingAttempts ?: 0,
                )
            } else {
                stringResource(R.string.authentication_error_credential_incorrect, methodLabel.orEmpty())
            }
        }

        is UnlockFailureMessage.RateLimited -> {
            stringResource(
                R.string.authentication_error_rate_limited,
                retryAfterSeconds,
            )
        }

        UnlockFailureMessage.BiometricKeyInvalidated -> {
            stringResource(R.string.authentication_key_invalidated)
        }

        UnlockFailureMessage.BiometricAuthenticationFailed -> {
            stringResource(R.string.authentication_error_biometric_failed)
        }

        UnlockFailureMessage.BiometricRateLimited -> {
            stringResource(R.string.authentication_error_biometric_rate_limited)
        }

        UnlockFailureMessage.AuthenticationBusy -> {
            stringResource(R.string.authentication_error_busy)
        }

        UnlockFailureMessage.AuthenticationHostUnavailable -> {
            stringResource(R.string.authentication_error_host_unavailable)
        }

        UnlockFailureMessage.AuthenticationMethodUnavailable -> {
            stringResource(R.string.authentication_error_method_unavailable)
        }

        UnlockFailureMessage.AuthenticationRestricted -> {
            stringResource(R.string.authentication_error_session_restricted)
        }

        UnlockFailureMessage.SessionUnavailable -> {
            stringResource(R.string.authentication_error_session_unavailable)
        }

        UnlockFailureMessage.AttemptStateUnavailable -> {
            stringResource(R.string.authentication_error_attempt_state_unavailable)
        }

        UnlockFailureMessage.PasswordTooShort -> {
            stringResource(
                R.string.authentication_error_password_too_short,
            )
        }

        UnlockFailureMessage.AppPasswordSetupFailed -> {
            stringResource(
                R.string.authentication_error_app_password_setup_failed,
            )
        }

        UnlockFailureMessage.Generic -> {
            stringResource(R.string.authentication_error_failed)
        }
    }
