package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftState

internal sealed interface RecoveryDraftMutation {
    data object AuthenticationStarted : RecoveryDraftMutation
    data object AuthenticationCancelled : RecoveryDraftMutation
    data object GenerationStarted : RecoveryDraftMutation
    data class DraftReady(val generationId: String) : RecoveryDraftMutation
    data object Committed : RecoveryDraftMutation
    data object Failed : RecoveryDraftMutation
    data object Dismissed : RecoveryDraftMutation
}

internal object RecoveryDraftReducer {
    fun reduce(
        state: RecoveryDraftState,
        mutation: RecoveryDraftMutation,
    ): RecoveryDraftState = when (mutation) {
        RecoveryDraftMutation.AuthenticationStarted -> RecoveryDraftState.Authenticating
        RecoveryDraftMutation.AuthenticationCancelled -> RecoveryDraftState.Empty
        RecoveryDraftMutation.GenerationStarted -> state.transitionFrom(
            expected = RecoveryDraftState.Authenticating,
            next = RecoveryDraftState.Generating,
        )
        is RecoveryDraftMutation.DraftReady -> state.transitionFrom(
            expected = RecoveryDraftState.Generating,
            next = RecoveryDraftState.Ready(mutation.generationId),
        )
        RecoveryDraftMutation.Committed -> if (state is RecoveryDraftState.Ready) {
            RecoveryDraftState.Committed
        } else {
            RecoveryDraftState.Failed
        }
        RecoveryDraftMutation.Failed -> RecoveryDraftState.Failed
        RecoveryDraftMutation.Dismissed -> RecoveryDraftState.Empty
    }

    private fun RecoveryDraftState.transitionFrom(
        expected: RecoveryDraftState,
        next: RecoveryDraftState,
    ): RecoveryDraftState = if (this == expected) next else RecoveryDraftState.Failed
}
