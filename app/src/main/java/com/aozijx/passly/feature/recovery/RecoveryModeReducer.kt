package com.aozijx.passly.feature.recovery

import com.aozijx.passly.domain.sensitive.SensitiveValue

internal sealed interface RecoveryModeMutation {
    data object PasswordDialogOpened : RecoveryModeMutation
    data class NewPasswordChanged(val value: SensitiveValue) : RecoveryModeMutation
    data class ConfirmPasswordChanged(val value: SensitiveValue) : RecoveryModeMutation
    data class ValidationFailed(val reason: RecoveryPasswordSetupFailure) : RecoveryModeMutation
    data object PasswordSetupStarted : RecoveryModeMutation
    data class PasswordSetupFailed(val reason: RecoveryPasswordSetupFailure) : RecoveryModeMutation
    data object PasswordSetupStopped : RecoveryModeMutation
    data object PasswordSetupCompleted : RecoveryModeMutation
    data object PasswordDialogDismissed : RecoveryModeMutation
    data object RecoveryModeRejected : RecoveryModeMutation
}

internal object RecoveryModeReducer {
    fun reduce(
        state: RecoveryModeUiState,
        mutation: RecoveryModeMutation,
    ): RecoveryModeUiState = when (mutation) {
        RecoveryModeMutation.PasswordDialogOpened -> state.copy(
            passwordSetup = RecoveryPasswordSetup.Editing(),
        )
        is RecoveryModeMutation.NewPasswordChanged -> state.updatePasswordSetup {
            copy(
                newPassword = mutation.value,
                phase = RecoveryPasswordSetupPhase.Idle,
            )
        }
        is RecoveryModeMutation.ConfirmPasswordChanged -> state.updatePasswordSetup {
            copy(
                confirmPassword = mutation.value,
                phase = RecoveryPasswordSetupPhase.Idle,
            )
        }
        is RecoveryModeMutation.ValidationFailed -> state.updatePasswordSetup {
            copy(phase = RecoveryPasswordSetupPhase.Failed(mutation.reason))
        }
        RecoveryModeMutation.PasswordSetupStarted -> state.updatePasswordSetup {
            copy(phase = RecoveryPasswordSetupPhase.Saving)
        }
        is RecoveryModeMutation.PasswordSetupFailed -> state.updatePasswordSetup {
            copy(phase = RecoveryPasswordSetupPhase.Failed(mutation.reason))
        }
        RecoveryModeMutation.PasswordSetupStopped -> state.updatePasswordSetup {
            copy(phase = RecoveryPasswordSetupPhase.Idle)
        }
        RecoveryModeMutation.PasswordSetupCompleted,
        RecoveryModeMutation.PasswordDialogDismissed,
        RecoveryModeMutation.RecoveryModeRejected -> state.copy(
            passwordSetup = RecoveryPasswordSetup.Closed,
        )
    }
}

private inline fun RecoveryModeUiState.updatePasswordSetup(
    transform: RecoveryPasswordSetup.Editing.() -> RecoveryPasswordSetup.Editing,
): RecoveryModeUiState {
    val editing = passwordSetup as? RecoveryPasswordSetup.Editing ?: return this
    return copy(passwordSetup = editing.transform())
}
