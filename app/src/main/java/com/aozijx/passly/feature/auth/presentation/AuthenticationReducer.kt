package com.aozijx.passly.feature.auth.presentation

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.auth.contract.AuthenticationUiState
import com.aozijx.passly.feature.auth.contract.AuthenticationVerificationFailure

internal sealed interface AuthenticationMutation {
    data class RecoveryUnlockVisibilityChanged(val visible: Boolean) : AuthenticationMutation
    data class AppPasswordChanged(val value: SensitiveValue) : AuthenticationMutation
    data class RecoveryCodeChanged(val value: SensitiveValue) : AuthenticationMutation
    data class NewAppPasswordChanged(val value: SensitiveValue) : AuthenticationMutation
    data class ConfirmAppPasswordChanged(val value: SensitiveValue) : AuthenticationMutation
    data class ExpandedMethodChanged(val method: AuthenticationMethod?) : AuthenticationMutation
    data class SetPasswordDialogVisibilityChanged(val visible: Boolean) : AuthenticationMutation
    data class AuthenticationStarted(val method: AuthenticationMethod) : AuthenticationMutation
    data class AuthenticationFailed(
        val method: AuthenticationMethod,
        val failure: AuthenticationFailure,
    ) : AuthenticationMutation
    data object AuthenticationFinished : AuthenticationMutation
    data object VerificationFailureCleared : AuthenticationMutation
    data object SetupStarted : AuthenticationMutation
    data class SetupFailed(val failure: AuthenticationFailure) : AuthenticationMutation
    data object SetupFinished : AuthenticationMutation
    data object SetupCompleted : AuthenticationMutation
    data object UnlockInputsReset : AuthenticationMutation
}

internal object AuthenticationReducer {
    fun reduce(
        state: AuthenticationUiState,
        mutation: AuthenticationMutation,
    ): AuthenticationUiState = when (mutation) {
        is AuthenticationMutation.RecoveryUnlockVisibilityChanged ->
            state.copy(recoveryUnlockVisible = mutation.visible)
        is AuthenticationMutation.AppPasswordChanged -> state.copy(
            appPassword = mutation.value,
            verificationFailure = null,
        )
        is AuthenticationMutation.RecoveryCodeChanged -> state.copy(
            recoveryCode = mutation.value,
            verificationFailure = null,
        )
        is AuthenticationMutation.NewAppPasswordChanged -> state.copy(
            newAppPassword = mutation.value,
            setupFailure = null,
        )
        is AuthenticationMutation.ConfirmAppPasswordChanged -> state.copy(
            confirmAppPassword = mutation.value,
            setupFailure = null,
        )
        is AuthenticationMutation.ExpandedMethodChanged -> state.copy(
            expandedMethod = mutation.method,
            verificationFailure = null,
        )
        is AuthenticationMutation.SetPasswordDialogVisibilityChanged -> state.copy(
            showSetPasswordDialog = mutation.visible,
            newAppPassword = if (mutation.visible) state.newAppPassword else EmptySensitiveValue,
            confirmAppPassword = if (mutation.visible) {
                state.confirmAppPassword
            } else {
                EmptySensitiveValue
            },
            setupFailure = null,
        )
        is AuthenticationMutation.AuthenticationStarted -> state.copy(
            activeMethod = mutation.method,
            verificationFailure = null,
        )
        is AuthenticationMutation.AuthenticationFailed -> state.copy(
            verificationFailure = AuthenticationVerificationFailure(
                method = mutation.method,
                failure = mutation.failure,
            ),
        )
        AuthenticationMutation.AuthenticationFinished -> state.copy(activeMethod = null)
        AuthenticationMutation.VerificationFailureCleared ->
            state.copy(verificationFailure = null)
        AuthenticationMutation.SetupStarted -> state.copy(
            isSettingAppPassword = true,
            setupFailure = null,
        )
        is AuthenticationMutation.SetupFailed -> state.copy(setupFailure = mutation.failure)
        AuthenticationMutation.SetupFinished -> state.copy(isSettingAppPassword = false)
        AuthenticationMutation.SetupCompleted -> state.copy(
            showSetPasswordDialog = false,
            newAppPassword = EmptySensitiveValue,
            confirmAppPassword = EmptySensitiveValue,
            setupFailure = null,
        )
        AuthenticationMutation.UnlockInputsReset -> state.copy(
            appPassword = EmptySensitiveValue,
            recoveryCode = EmptySensitiveValue,
            recoveryUnlockVisible = false,
            expandedMethod = null,
            verificationFailure = null,
        )
    }
}
