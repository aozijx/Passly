package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import com.aozijx.passly.presentation.mapping.PasswordStrengthState

internal sealed interface RecoveryModeMutation {
    data object PasswordDialogOpened : RecoveryModeMutation

    data class NewPasswordChanged(
        val value: SensitiveValue,
        val passwordStrength: PasswordStrengthState,
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
                if (state.passwordSetup is RecoveryPasswordSetup.Saving) return state
                state.copy(passwordSetup = RecoveryPasswordSetup.Editing(PasswordSetupDraft()))
            }

            is RecoveryModeMutation.NewPasswordChanged ->
                state.updateEditableDraft(mutation.value) { copy(newPassword = mutation.value, passwordStrength = mutation.passwordStrength) }

            is RecoveryModeMutation.ConfirmPasswordChanged ->
                state.updateEditableDraft(mutation.value) { copy(confirmPassword = mutation.value) }

            is RecoveryModeMutation.ValidationFailed ->
                state.updateEditableSetup { draft -> RecoveryPasswordSetup.Failed(draft, mutation.reason) }

            RecoveryModeMutation.PasswordSetupStarted ->
                state.updateEditableSetup { RecoveryPasswordSetup.Saving(PasswordSetupDraft()) }

            is RecoveryModeMutation.PasswordSetupFailed ->
                when (val setup = state.passwordSetup) {
                    is RecoveryPasswordSetup.Saving -> state.copy(passwordSetup = RecoveryPasswordSetup.Failed(setup.draft, mutation.reason))
                    else -> state
                }

            RecoveryModeMutation.PasswordSetupStopped ->
                when (val setup = state.passwordSetup) {
                    is RecoveryPasswordSetup.Saving -> state.copy(passwordSetup = RecoveryPasswordSetup.Editing(setup.draft))
                    else -> state
                }

            RecoveryModeMutation.PasswordSetupCompleted,
            RecoveryModeMutation.RecoveryModeRejected,
            RecoveryModeMutation.PasswordDialogDiscarded,
            -> state.copy(passwordSetup = RecoveryPasswordSetup.Closed)

            RecoveryModeMutation.PasswordDialogDismissed -> {
                if (state.passwordSetup is RecoveryPasswordSetup.Saving) return state
                state.copy(passwordSetup = RecoveryPasswordSetup.Closed)
            }
        }
}

private inline fun RecoveryModeUiState.updateEditableDraft(
    value: SensitiveValue,
    transform: PasswordSetupDraft.() -> PasswordSetupDraft,
): RecoveryModeUiState =
    when (val setup = passwordSetup) {
        RecoveryPasswordSetup.Closed -> {
            value.wipe()
            this
        }
        is RecoveryPasswordSetup.Saving -> {
            value.wipe()
            this
        }
        is RecoveryPasswordSetup.Editing -> copy(passwordSetup = RecoveryPasswordSetup.Editing(setup.draft.transform()))
        is RecoveryPasswordSetup.Failed -> copy(passwordSetup = RecoveryPasswordSetup.Editing(setup.draft.transform()))
    }

private inline fun RecoveryModeUiState.updateEditableSetup(
    transform: (PasswordSetupDraft) -> RecoveryPasswordSetup,
): RecoveryModeUiState =
    when (val setup = passwordSetup) {
        RecoveryPasswordSetup.Closed -> this
        is RecoveryPasswordSetup.Editing -> copy(passwordSetup = transform(setup.draft))
        is RecoveryPasswordSetup.Failed -> copy(passwordSetup = transform(setup.draft))
        is RecoveryPasswordSetup.Saving -> this
    }
