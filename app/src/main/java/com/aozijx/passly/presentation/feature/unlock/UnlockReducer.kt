package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.unlock.UnlockUiState
import com.aozijx.passly.presentation.feature.unlock.UnlockVerificationFailure

internal sealed interface UnlockMutation {
    data class AvailableMethodsChanged(val methods: AuthenticationMethods) : UnlockMutation
    data class RecoveryUnlockVisibilityChanged(val visible: Boolean) : UnlockMutation
    data class AppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data class RecoveryCodeChanged(val value: SensitiveValue) : UnlockMutation
    data class ExpandedMethodChanged(val method: AuthenticationMethod?) : UnlockMutation
    data class AuthenticationStarted(val method: AuthenticationMethod) : UnlockMutation
    data class AuthenticationFailed(
        val method: AuthenticationMethod,
        val failure: AuthenticationFailure,
    ) : UnlockMutation
    data object AuthenticationFinished : UnlockMutation
    data object VerificationFailureCleared : UnlockMutation
    data object UnlockInputsReset : UnlockMutation
    data class NewAppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data class ConfirmAppPasswordChanged(val value: SensitiveValue) : UnlockMutation
    data class SetPasswordDialogVisibilityChanged(val visible: Boolean) : UnlockMutation
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
        is UnlockMutation.AvailableMethodsChanged -> state.copy(
            availableMethods = mutation.methods,
        )
        is UnlockMutation.RecoveryUnlockVisibilityChanged ->
            state.copy(recoveryUnlockVisible = mutation.visible)
        is UnlockMutation.AppPasswordChanged -> state.copy(
            appPassword = mutation.value,
            verificationFailure = null,
        )
        is UnlockMutation.RecoveryCodeChanged -> state.copy(
            recoveryCode = mutation.value,
            verificationFailure = null,
        )
        is UnlockMutation.ExpandedMethodChanged -> state.copy(
            expandedMethod = mutation.method,
            verificationFailure = null,
        )
        is UnlockMutation.AuthenticationStarted -> state.copy(
            activeMethod = mutation.method,
            verificationFailure = null,
        )
        is UnlockMutation.AuthenticationFailed -> state.copy(
            verificationFailure = UnlockVerificationFailure(
                method = mutation.method,
                message = mutation.failure.toVerificationMessage(),
            ),
        )
        UnlockMutation.AuthenticationFinished -> state.copy(activeMethod = null)
        UnlockMutation.VerificationFailureCleared ->
            state.copy(verificationFailure = null)
        UnlockMutation.UnlockInputsReset -> state.copy(
            appPassword = EmptySensitiveValue,
            recoveryCode = EmptySensitiveValue,
            recoveryUnlockVisible = false,
            expandedMethod = null,
            verificationFailure = null,
        )
        is UnlockMutation.NewAppPasswordChanged -> state.withSetupPasswords(
            newPassword = mutation.value,
            confirmPassword = state.confirmAppPassword,
        )
        is UnlockMutation.ConfirmAppPasswordChanged -> state.withSetupPasswords(
            newPassword = state.newAppPassword,
            confirmPassword = mutation.value,
        )
        is UnlockMutation.SetPasswordDialogVisibilityChanged -> state.copy(
            showSetPasswordDialog = mutation.visible,
            newAppPassword = if (mutation.visible) state.newAppPassword else EmptySensitiveValue,
            confirmAppPassword = if (mutation.visible) state.confirmAppPassword else EmptySensitiveValue,
            canConfirmAppPassword = false,
            setupFailure = null,
        )
        UnlockMutation.PasswordSetupStarted -> state.copy(
            isSettingAppPassword = true,
            setupFailure = null,
        )
        is UnlockMutation.PasswordSetupFailed -> state.copy(
            setupFailure = mutation.failure.toSetupMessage(),
        )
        UnlockMutation.PasswordSetupFinished -> state.copy(isSettingAppPassword = false)
        UnlockMutation.PasswordSetupCompleted -> state.copy(
            showSetPasswordDialog = false,
            newAppPassword = EmptySensitiveValue,
            confirmAppPassword = EmptySensitiveValue,
            canConfirmAppPassword = false,
            setupFailure = null,
        )
    }

    private fun UnlockUiState.withSetupPasswords(
        newPassword: SensitiveValue,
        confirmPassword: SensitiveValue,
    ): UnlockUiState = copy(
        newAppPassword = newPassword,
        confirmAppPassword = confirmPassword,
        canConfirmAppPassword = passwordsCanBeConfirmed(newPassword, confirmPassword),
        setupFailure = null,
    )

    private fun passwordsCanBeConfirmed(
        password: SensitiveValue,
        confirmation: SensitiveValue,
    ): Boolean {
        val passwordChars = password.toCharArray()
        val confirmationChars = confirmation.toCharArray()
        return try {
            AppPasswordPolicy.DEFAULT.acceptsLength(passwordChars.size) &&
                passwordChars.contentEquals(confirmationChars)
        } finally {
            MemoryCleaner.wipeCharArray(passwordChars)
            MemoryCleaner.wipeCharArray(confirmationChars)
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
        AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION ->
            UnlockFailureMessage.PasswordTooShort
        AuthenticationFailureCode.RATE_LIMITED -> UnlockFailureMessage.RateLimited(
            retryAfterSeconds = (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L),
        )
        else -> UnlockFailureMessage.AppPasswordSetupFailed
    }
}
