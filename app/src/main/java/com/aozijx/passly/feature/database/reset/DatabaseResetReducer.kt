package com.aozijx.passly.feature.database.reset

internal object DatabaseResetReducer {
    fun reduce(
        state: DatabaseResetUiState,
        mutation: DatabaseResetMutation,
    ): DatabaseResetUiState = when (mutation) {
        DatabaseResetMutation.ResetStarted -> state.copy(
            isResetting = true,
            isResetComplete = false,
            error = null,
        )
        DatabaseResetMutation.ResetCancelled -> state.copy(
            isResetting = false,
        )
        DatabaseResetMutation.ResetCompleted -> state.copy(
            isResetting = false,
            isResetComplete = true,
            error = null,
        )
        is DatabaseResetMutation.ResetFailed -> state.copy(
            isResetting = false,
            error = mutation.message,
        )
    }
}
