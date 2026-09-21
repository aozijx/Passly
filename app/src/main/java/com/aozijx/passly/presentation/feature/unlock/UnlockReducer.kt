package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.unlock.UnlockUiState
import com.aozijx.passly.presentation.feature.unlock.UnlockVerificationFailure

internal sealed interface UnlockMutation {
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
                failure = mutation.failure,
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
        is UnlockMutation.NewAppPasswordChanged -> state.copy(
            newAppPassword = mutation.value,
            setupFailure = null,
        )
        is UnlockMutation.ConfirmAppPasswordChanged -> state.copy(
            confirmAppPassword = mutation.value,
            setupFailure = null,
        )
        is UnlockMutation.SetPasswordDialogVisibilityChanged -> state.copy(
            showSetPasswordDialog = mutation.visible,
            newAppPassword = if (mutation.visible) state.newAppPassword else EmptySensitiveValue,
            confirmAppPassword = if (mutation.visible) state.confirmAppPassword else EmptySensitiveValue,
            setupFailure = null,
        )
        UnlockMutation.PasswordSetupStarted -> state.copy(
            isSettingAppPassword = true,
            setupFailure = null,
        )
        is UnlockMutation.PasswordSetupFailed -> state.copy(setupFailure = mutation.failure)
        UnlockMutation.PasswordSetupFinished -> state.copy(isSettingAppPassword = false)
        UnlockMutation.PasswordSetupCompleted -> state.copy(
            showSetPasswordDialog = false,
            newAppPassword = EmptySensitiveValue,
            confirmAppPassword = EmptySensitiveValue,
            setupFailure = null,
        )
    }
}
