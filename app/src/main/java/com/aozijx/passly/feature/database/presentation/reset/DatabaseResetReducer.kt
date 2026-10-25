package com.aozijx.passly.feature.database.presentation.reset

internal object DatabaseResetReducer {
    fun reduce(
        state: DatabaseResetUiState,
        mutation: DatabaseResetMutation,
    ): DatabaseResetUiState = when (mutation) {
        DatabaseResetMutation.ResetStarted -> state.copy(phase = DatabaseResetPhase.Resetting)
        DatabaseResetMutation.ResetCancelled -> state.copy(phase = DatabaseResetPhase.Idle)
        DatabaseResetMutation.ResetCompleted -> state.copy(phase = DatabaseResetPhase.Completed)
        is DatabaseResetMutation.ResetFailed -> state.copy(
            phase = DatabaseResetPhase.Failed(mutation.reason),
        )
    }
}
