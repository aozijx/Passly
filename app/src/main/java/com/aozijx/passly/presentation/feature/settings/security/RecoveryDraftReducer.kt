package com.aozijx.passly.presentation.feature.settings.security

internal sealed interface RecoveryDraftMutation {
    data object CreationStarted : RecoveryDraftMutation
    data object CreationCancelled : RecoveryDraftMutation
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
        RecoveryDraftMutation.CreationStarted -> RecoveryDraftState.Creating
        RecoveryDraftMutation.CreationCancelled -> RecoveryDraftState.Empty
        is RecoveryDraftMutation.DraftReady -> state.transitionFrom(
            expected = RecoveryDraftState.Creating,
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
