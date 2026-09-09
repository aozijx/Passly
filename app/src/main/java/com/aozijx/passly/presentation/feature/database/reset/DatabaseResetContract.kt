package com.aozijx.passly.presentation.feature.database.reset

data class DatabaseResetUiState(
    val isResetting: Boolean = false,
    val isResetComplete: Boolean = false,
    val error: String? = null,
)

sealed interface DatabaseResetUiAction {
    data object Reset : DatabaseResetUiAction
}

internal sealed interface DatabaseResetMutation {
    data object ResetStarted : DatabaseResetMutation
    data object ResetCancelled : DatabaseResetMutation
    data object ResetCompleted : DatabaseResetMutation
    data class ResetFailed(val message: String) : DatabaseResetMutation
}
