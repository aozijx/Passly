package com.aozijx.passly.feature.database.presentation.reset

internal object DatabaseResetReducer {
    fun reduce(
        state: DatabaseResetUiState,
        mutation: DatabaseResetMutation,
    ): DatabaseResetUiState =
        when (mutation) {
            DatabaseResetMutation.PageBoundaryCrossed ->
                if (state is DatabaseResetUiState.Resetting) state else DatabaseResetUiState.Idle

            DatabaseResetMutation.ResetStarted -> DatabaseResetUiState.Resetting

            DatabaseResetMutation.ResetCancelled -> DatabaseResetUiState.Idle

            DatabaseResetMutation.ResetCompleted -> DatabaseResetUiState.Completed

            DatabaseResetMutation.ResetSuccessFeedbackElapsed ->
                if (state == DatabaseResetUiState.Completed) DatabaseResetUiState.Idle else state

            is DatabaseResetMutation.ResetFailed -> DatabaseResetUiState.Failed(mutation.reason)
        }
}
