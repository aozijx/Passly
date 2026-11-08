package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

internal sealed interface RecoveryModeMutation {
    data object PasswordDialogOpened : RecoveryModeMutation

    data class NewPasswordChanged(
        val value: SensitiveValue,
        val passwordStrength: PasswordStrengthResult?,
    ) : RecoveryModeMutation

    data class ConfirmPasswordChanged(
        val value: SensitiveValue,
    ) : RecoveryModeMutation

    data class ValidationFailed(
        val reason: RecoveryPasswordSetupFailure,
    ) : RecoveryModeMutation

    data object PasswordSetupStarted : RecoveryModeMutation

    data class PasswordSetupFailed(
        val reason: RecoveryPasswordSetupFailure,
    ) : RecoveryModeMutation

    data object PasswordSetupStopped : RecoveryModeMutation

    data object PasswordSetupCompleted : RecoveryModeMutation

    data object PasswordDialogDismissed : RecoveryModeMutation

    data object PasswordDialogDiscarded : RecoveryModeMutation

    data object RecoveryModeRejected : RecoveryModeMutation
}

internal object RecoveryModeReducer {
    fun reduce(
        state: RecoveryModeUiState,
        mutation: RecoveryModeMutation,
    ): RecoveryModeUiState =
        when (mutation) {
            RecoveryModeMutation.PasswordDialogOpened -> {
                if (state is RecoveryModeUiState.Saving) return state
                RecoveryModeUiState.Editing(PasswordSetupDraft())
            }

            is RecoveryModeMutation.NewPasswordChanged ->
                state.updateEditableDraft(mutation.value) { copy(newPassword = mutation.value, passwordStrength = mutation.passwordStrength) }

            is RecoveryModeMutation.ConfirmPasswordChanged ->
                state.updateEditableDraft(mutation.value) { copy(confirmPassword = mutation.value) }

            is RecoveryModeMutation.ValidationFailed ->
                state.updateEditableSetup { draft -> RecoveryModeUiState.Failed(draft, mutation.reason) }

            RecoveryModeMutation.PasswordSetupStarted ->
                state.updateEditableSetup { RecoveryModeUiState.Saving(PasswordSetupDraft()) }

            is RecoveryModeMutation.PasswordSetupFailed ->
                when (state) {
                    is RecoveryModeUiState.Saving -> RecoveryModeUiState.Failed(state.draft, mutation.reason)
                    else -> state
                }

            RecoveryModeMutation.PasswordSetupStopped ->
                when (state) {
                    is RecoveryModeUiState.Saving -> RecoveryModeUiState.Editing(state.draft)
                    else -> state
                }

            RecoveryModeMutation.PasswordSetupCompleted,
            RecoveryModeMutation.RecoveryModeRejected,
            RecoveryModeMutation.PasswordDialogDiscarded,
            -> RecoveryModeUiState.Closed

            RecoveryModeMutation.PasswordDialogDismissed -> {
                if (state is RecoveryModeUiState.Saving) return state
                RecoveryModeUiState.Closed
            }
        }
}

private inline fun RecoveryModeUiState.updateEditableDraft(
    value: SensitiveValue,
    transform: PasswordSetupDraft.() -> PasswordSetupDraft,
): RecoveryModeUiState =
    when (this) {
        RecoveryModeUiState.Closed -> {
            value.wipe()
            this
        }
        is RecoveryModeUiState.Saving -> {
            value.wipe()
            this
        }
        is RecoveryModeUiState.Editing -> RecoveryModeUiState.Editing(draft.transform())
        is RecoveryModeUiState.Failed -> RecoveryModeUiState.Editing(draft.transform())
    }

private inline fun RecoveryModeUiState.updateEditableSetup(
    transform: (PasswordSetupDraft) -> RecoveryModeUiState,
): RecoveryModeUiState =
    when (this) {
        RecoveryModeUiState.Closed,
        is RecoveryModeUiState.Saving,
        -> this
        is RecoveryModeUiState.Editing -> transform(draft)
        is RecoveryModeUiState.Failed -> transform(draft)
    }
