package com.aozijx.passly.feature.auth.presentation

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.auth.contract.UnlockUiState
import com.aozijx.passly.feature.auth.contract.UnlockVerificationFailure

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
    }
}
