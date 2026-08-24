package com.aozijx.passly.presentation.feature.recovery

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.recovery.RecoveryModeUiState

internal sealed interface RecoveryModeMutation {
    data object PasswordDialogOpened : RecoveryModeMutation
    data class NewPasswordChanged(val value: SensitiveValue) : RecoveryModeMutation
    data class ConfirmPasswordChanged(val value: SensitiveValue) : RecoveryModeMutation
    data class ValidationFailed(val message: String) : RecoveryModeMutation
    data object PasswordSetupStarted : RecoveryModeMutation
    data class PasswordSetupFailed(val message: String) : RecoveryModeMutation
    data object PasswordSetupStopped : RecoveryModeMutation
    data object PasswordSetupCompleted : RecoveryModeMutation
    data object PasswordDialogDismissed : RecoveryModeMutation
    data class RecoveryModeRejected(val message: String) : RecoveryModeMutation
}

internal object RecoveryModeReducer {
    fun reduce(
        state: RecoveryModeUiState,
        mutation: RecoveryModeMutation,
    ): RecoveryModeUiState = when (mutation) {
        RecoveryModeMutation.PasswordDialogOpened -> state.copy(
            showSetPasswordDialog = true,
            passwordSetupError = null,
        )
        is RecoveryModeMutation.NewPasswordChanged -> state.copy(
            newPassword = mutation.value,
            passwordSetupError = null,
        )
        is RecoveryModeMutation.ConfirmPasswordChanged -> state.copy(
            confirmPassword = mutation.value,
            passwordSetupError = null,
        )
        is RecoveryModeMutation.ValidationFailed ->
            state.copy(passwordSetupError = mutation.message)
        RecoveryModeMutation.PasswordSetupStarted -> state.copy(
            isSettingPassword = true,
            passwordSetupError = null,
        )
        is RecoveryModeMutation.PasswordSetupFailed -> state.copy(
            isSettingPassword = false,
            passwordSetupError = mutation.message,
        )
        RecoveryModeMutation.PasswordSetupStopped -> state.copy(isSettingPassword = false)
        RecoveryModeMutation.PasswordSetupCompleted -> state.copy(
            showSetPasswordDialog = false,
            newPassword = EmptySensitiveValue,
            confirmPassword = EmptySensitiveValue,
            isSettingPassword = false,
            passwordSetupError = null,
        )
        RecoveryModeMutation.PasswordDialogDismissed -> state.copy(
            showSetPasswordDialog = false,
            newPassword = EmptySensitiveValue,
            confirmPassword = EmptySensitiveValue,
            passwordSetupError = null,
        )
        is RecoveryModeMutation.RecoveryModeRejected -> state.copy(
            showSetPasswordDialog = false,
            newPassword = EmptySensitiveValue,
            confirmPassword = EmptySensitiveValue,
            isSettingPassword = false,
            passwordSetupError = mutation.message,
        )
    }
}
