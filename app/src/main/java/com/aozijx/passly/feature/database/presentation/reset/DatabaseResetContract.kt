package com.aozijx.passly.feature.database.presentation.reset

import com.aozijx.passly.presentation.shared.error.UiErrorMessage

data class DatabaseResetUiState(
    val phase: DatabaseResetPhase = DatabaseResetPhase.Idle,
)

sealed interface DatabaseResetPhase {
    data object Idle : DatabaseResetPhase
    data object Resetting : DatabaseResetPhase
    data object Completed : DatabaseResetPhase
    data class Failed(val reason: DatabaseResetFailure) : DatabaseResetPhase
}

sealed interface DatabaseResetFailure {
    data object SessionRestricted : DatabaseResetFailure
    data object AuthorizationDenied : DatabaseResetFailure
    data class Unexpected(val message: UiErrorMessage) : DatabaseResetFailure
}

sealed interface DatabaseResetUiAction {
    data object Reset : DatabaseResetUiAction
}

internal sealed interface DatabaseResetMutation {
    data object ResetStarted : DatabaseResetMutation
    data object ResetCancelled : DatabaseResetMutation
    data object ResetCompleted : DatabaseResetMutation
    data class ResetFailed(val reason: DatabaseResetFailure) : DatabaseResetMutation
}
