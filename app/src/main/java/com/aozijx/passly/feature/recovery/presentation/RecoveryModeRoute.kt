package com.aozijx.passly.feature.recovery.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.recovery.presentation.ui.RecoveryModeScreen
import com.aozijx.passly.feature.recovery.presentation.ui.RecoveryPasswordDialog
import com.aozijx.passly.presentation.shared.event.collectPageEffects

@Composable
fun RecoveryModeRoute(onExit: () -> Unit) {
    val viewModel: RecoveryModeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }
    LaunchedEffect(viewModel, lifecycle) {
        collectPageEffects(lifecycle, viewModel.effect) { effect ->
            when (effect) {
                RecoveryModeEffect.ExitRecovery -> {
                    onExit()
                }

                RecoveryModeEffect.PasswordResetCompleted -> {
                    Toast
                        .makeText(
                            context,
                            R.string.recovery_mode_password_reset_success,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }
    val passwordSetup = state.passwordSetup
    val passwordDraft =
        when (passwordSetup) {
            RecoveryPasswordSetup.Closed -> null
            is RecoveryPasswordSetup.Editing -> passwordSetup.draft
            is RecoveryPasswordSetup.Saving -> passwordSetup.draft
            is RecoveryPasswordSetup.Failed -> passwordSetup.draft
        }
    RecoveryModeScreen(
        isSettingPassword = passwordSetup is RecoveryPasswordSetup.Saving,
        onSetPassword = { viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked) },
        onExit = { viewModel.onAction(RecoveryModeUiAction.ExitClicked) },
    )
    if (passwordDraft != null) {
        val newPassword = passwordDraft.newPassword.toUiString()
        val confirmPassword = passwordDraft.confirmPassword.toUiString()
        val isSaving = passwordSetup is RecoveryPasswordSetup.Saving
        RecoveryPasswordDialog(
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            confirmationState = passwordDraft.confirmationState,
            strength = passwordDraft.passwordStrength.toRecoveryPasswordStrengthUiState(MaterialTheme.colorScheme),
            confirmEnabled = passwordDraft.canSubmit(),
            onNewPasswordChange = { viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(it)) },
            onConfirmPasswordChange = { viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(it)) },
            onConfirm = { viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword) },
            onDismiss = { viewModel.onAction(RecoveryModeUiAction.DismissPasswordDialog) },
            isBusy = isSaving,
            errorMessage = (passwordSetup as? RecoveryPasswordSetup.Failed)?.reason?.message(),
        )
    }
}

@Composable
private fun RecoveryPasswordSetupFailure.message(): String =
    when (this) {
        RecoveryPasswordSetupFailure.PasswordMismatchOrEmpty -> {
            stringResource(R.string.recovery_mode_password_mismatch_or_empty)
        }

        RecoveryPasswordSetupFailure.SetupFailed -> {
            stringResource(R.string.recovery_mode_password_setup_failed)
        }
    }

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
