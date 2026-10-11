package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

internal sealed interface UnlockMutation {
    data class AvailableMethodsChanged(val methods: AuthenticationMethods) : UnlockMutation
    data class RecoveryUnlockVisibilityChanged(val visible: Boolean) : UnlockMutation
    data class AppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data class RecoveryCodeChanged(val value: SensitiveValue) : UnlockMutation
    data class InputExpansionChanged(val expansion: UnlockInputExpansion) : UnlockMutation
    data class AuthenticationStarted(val method: AuthenticationMethod) : UnlockMutation
    data class AuthenticationFailed(
        val method: AuthenticationMethod,
        val failure: AuthenticationFailure,
    ) : UnlockMutation
    data object AuthenticationFinished : UnlockMutation
    data object VerificationFailureCleared : UnlockMutation
    data object UnlockInputsReset : UnlockMutation
    data object PasswordSetupOpened : UnlockMutation
    data object PasswordSetupClosed : UnlockMutation
    data class NewAppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data class ConfirmAppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data object PasswordSetupStarted : UnlockMutation
    data class PasswordSetupFailed(val failure: AuthenticationFailure) : UnlockMutation
    data object PasswordSetupFinished : UnlockMutation
    data object PasswordSetupCompleted : UnlockMutation
}

internal object UnlockReducer {
    fun reduce(
        state: UnlockUiState,
        mutation: UnlockMutation,
    ): UnlockUiState = when (mutation) {
        is UnlockMutation.AvailableMethodsChanged -> state.copy(availableMethods = mutation.methods)
        is UnlockMutation.RecoveryUnlockVisibilityChanged ->
            state.copy(recoveryUnlockVisible = mutation.visible)
        is UnlockMutation.AppPasswordChanged -> state.copy(
            appPassword = mutation.value,
            verificationPhase = state.verificationPhase.withFailureCleared(),
        )
        is UnlockMutation.RecoveryCodeChanged -> state.copy(
            recoveryCode = mutation.value,
            verificationPhase = state.verificationPhase.withFailureCleared(),
        )
        is UnlockMutation.InputExpansionChanged -> state.copy(
            inputExpansion = mutation.expansion,
            verificationPhase = state.verificationPhase.withFailureCleared(),
        )
        is UnlockMutation.AuthenticationStarted -> state.copy(
            verificationPhase = UnlockVerificationPhase.Authenticating(mutation.method),
        )
        is UnlockMutation.AuthenticationFailed -> state.copy(
            verificationPhase = UnlockVerificationPhase.Failed(
                UnlockVerificationFailure(
                    method = mutation.method,
                    message = mutation.failure.toVerificationMessage(),
                ),
            ),
        )
        UnlockMutation.AuthenticationFinished -> state.copy(
            verificationPhase = when (state.verificationPhase) {
                is UnlockVerificationPhase.Authenticating -> UnlockVerificationPhase.Idle
                is UnlockVerificationPhase.Failed,
                UnlockVerificationPhase.Idle -> state.verificationPhase
            },
        )
        UnlockMutation.VerificationFailureCleared -> state.copy(
            verificationPhase = state.verificationPhase.withFailureCleared(),
        )
        UnlockMutation.UnlockInputsReset -> state.copy(
            appPassword = EmptySensitiveValue,
            recoveryCode = EmptySensitiveValue,
            recoveryUnlockVisible = false,
            inputExpansion = UnlockInputExpansion.Collapsed,
            verificationPhase = UnlockVerificationPhase.Idle,
        )
        UnlockMutation.PasswordSetupOpened -> state.copy(
            passwordSetup = UnlockPasswordSetup.Editing(),
        )
        UnlockMutation.PasswordSetupClosed,
        UnlockMutation.PasswordSetupCompleted -> state.copy(
            passwordSetup = UnlockPasswordSetup.Closed,
        )
        is UnlockMutation.NewAppPasswordChanged -> state.updatePasswordSetup {
            copy(
                newPassword = mutation.value,
                phase = UnlockPasswordSetupPhase.Idle,
            )
        }
        is UnlockMutation.ConfirmAppPasswordChanged -> state.updatePasswordSetup {
            copy(
                confirmPassword = mutation.value,
                phase = UnlockPasswordSetupPhase.Idle,
            )
        }
        UnlockMutation.PasswordSetupStarted -> state.updatePasswordSetup {
            copy(phase = UnlockPasswordSetupPhase.Saving)
        }
        is UnlockMutation.PasswordSetupFailed -> state.updatePasswordSetup {
            copy(phase = UnlockPasswordSetupPhase.Failed(mutation.failure.toSetupMessage()))
        }
        UnlockMutation.PasswordSetupFinished -> state.updatePasswordSetup {
            if (phase == UnlockPasswordSetupPhase.Saving) {
                copy(phase = UnlockPasswordSetupPhase.Idle)
            } else {
                this
            }
        }
    }

    private fun AuthenticationFailure.toVerificationMessage(): UnlockFailureMessage = when (code) {
        AuthenticationFailureCode.CREDENTIAL_INCORRECT ->
            UnlockFailureMessage.IncorrectCredential(attempts.remaining)
        AuthenticationFailureCode.RATE_LIMITED -> UnlockFailureMessage.RateLimited(
            retryAfterSeconds = (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L),
        )
        else -> UnlockFailureMessage.Generic
    }

    private fun AuthenticationFailure.toSetupMessage(): UnlockFailureMessage = when (code) {
        AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION -> UnlockFailureMessage.PasswordTooShort
        AuthenticationFailureCode.RATE_LIMITED -> UnlockFailureMessage.RateLimited(
            retryAfterSeconds = (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L),
        )
        else -> UnlockFailureMessage.AppPasswordSetupFailed
    }
}

private fun UnlockVerificationPhase.withFailureCleared(): UnlockVerificationPhase = when (this) {
    is UnlockVerificationPhase.Failed -> UnlockVerificationPhase.Idle
    is UnlockVerificationPhase.Authenticating,
    UnlockVerificationPhase.Idle -> this
}

private inline fun UnlockUiState.updatePasswordSetup(
    transform: UnlockPasswordSetup.Editing.() -> UnlockPasswordSetup.Editing,
): UnlockUiState {
    val editing = passwordSetup as? UnlockPasswordSetup.Editing ?: return this
    return copy(passwordSetup = editing.transform())
}
