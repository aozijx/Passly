package com.aozijx.passly.feature.recovery

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.recovery.ui.RecoveryModeScreen
import com.aozijx.passly.feature.recovery.ui.RecoveryPasswordDialog
import com.aozijx.passly.feature.recovery.ui.RecoveryPasswordStrengthTone
import com.aozijx.passly.feature.recovery.ui.RecoveryPasswordStrengthUiModel

@Composable
fun RecoveryModeRoute(onExit: () -> Unit) {
    val viewModel: RecoveryModeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RecoveryModeEffect.ExitRecovery -> onExit()
                RecoveryModeEffect.PasswordResetCompleted -> Toast.makeText(
                    context,
                    R.string.recovery_mode_password_reset_success,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
    val passwordSetup = state.passwordSetup
    RecoveryModeScreen(
        isSettingPassword = passwordSetup is RecoveryPasswordSetup.Editing &&
            passwordSetup.phase == RecoveryPasswordSetupPhase.Saving,
        onSetPassword = { viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked) },
        onExit = { viewModel.onAction(RecoveryModeUiAction.ExitClicked) },
    )
    if (passwordSetup is RecoveryPasswordSetup.Editing) {
        val newPassword = passwordSetup.newPassword.toUiString()
        val confirmPassword = passwordSetup.confirmPassword.toUiString()
        val isSaving = passwordSetup.phase == RecoveryPasswordSetupPhase.Saving
        RecoveryPasswordDialog(
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            strength = PasswordStrengthEvaluator.evaluate(newPassword).toUiModel(),
            confirmEnabled = AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length) &&
                newPassword == confirmPassword,
            onNewPasswordChange = { viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(it)) },
            onConfirmPasswordChange = { viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(it)) },
            onConfirm = { viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword) },
            onDismiss = { viewModel.onAction(RecoveryModeUiAction.DismissPasswordDialog) },
            isBusy = isSaving,
            errorMessage = (passwordSetup.phase as? RecoveryPasswordSetupPhase.Failed)
                ?.reason
                ?.message(),
        )
    }
}

@Composable
private fun RecoveryPasswordSetupFailure.message(): String = when (this) {
    RecoveryPasswordSetupFailure.PasswordMismatchOrEmpty ->
        stringResource(R.string.recovery_mode_password_mismatch_or_empty)
    RecoveryPasswordSetupFailure.SetupFailed ->
        stringResource(R.string.recovery_mode_password_setup_failed)
}

private fun PasswordStrengthResult.toUiModel() = RecoveryPasswordStrengthUiModel(
    score = score,
    labelRes = when (level) {
        PasswordStrengthLevel.VERY_WEAK -> R.string.recovery_mode_password_strength_very_weak
        PasswordStrengthLevel.WEAK -> R.string.recovery_mode_password_strength_weak
        PasswordStrengthLevel.MEDIUM -> R.string.recovery_mode_password_strength_medium
        PasswordStrengthLevel.GOOD -> R.string.recovery_mode_password_strength_good
        PasswordStrengthLevel.STRONG -> R.string.recovery_mode_password_strength_strong
    },
    tone = RecoveryPasswordStrengthTone.entries[level.ordinal],
)

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
