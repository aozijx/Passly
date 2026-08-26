package com.aozijx.passly.presentation.feature.recovery

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.recovery.RecoveryModeContent
import com.aozijx.passly.presentation.ui.recovery.RecoveryPasswordDialog
import com.aozijx.passly.presentation.ui.recovery.RecoveryPasswordStrengthTone
import com.aozijx.passly.presentation.ui.recovery.RecoveryPasswordStrengthUiModel

@Composable
fun RecoveryModeScreen(viewModel: RecoveryModeViewModel, onExit: () -> Unit) {
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
    RecoveryModeContent(
        isSettingPassword = state.isSettingPassword,
        onSetPassword = { viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked) },
        onExit = { viewModel.onAction(RecoveryModeUiAction.ExitClicked) },
    )
    if (state.showSetPasswordDialog) {
        val newPassword = state.newPassword.toUiString()
        val confirmPassword = state.confirmPassword.toUiString()
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
            isBusy = state.isSettingPassword,
            errorMessage = state.passwordSetupError,
        )
    }
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
