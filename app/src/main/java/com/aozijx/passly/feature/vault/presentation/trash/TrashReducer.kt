package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.domain.entry.model.query.EntryListItem

internal sealed interface TrashMutation {
    data object LoadStarted : TrashMutation
    data class Loaded(val entries: List<EntryListItem>) : TrashMutation
    data class LoadFailed(val message: String) : TrashMutation
    data object ErrorCleared : TrashMutation
    data class EntryActionStarted(val entryId: String) : TrashMutation
    data object EntryActionFinished : TrashMutation
    data object EmptyStarted : TrashMutation
    data object EmptyFinished : TrashMutation
    data class ActionFailed(val message: String) : TrashMutation
}

internal object TrashReducer {
    fun reduce(state: TrashUiState, mutation: TrashMutation): TrashUiState = when (mutation) {
        TrashMutation.LoadStarted -> state.copy(content = TrashContentState.Loading)
        is TrashMutation.Loaded -> state.copy(content = TrashContentState.Loaded(mutation.entries))
        is TrashMutation.LoadFailed -> state.copy(content = TrashContentState.Failed(mutation.message))
        TrashMutation.ErrorCleared -> state.copy(
            operation = when (state.operation) {
                is TrashOperationState.Failed -> TrashOperationState.Idle
                TrashOperationState.Emptying,
                is TrashOperationState.Entry,
                TrashOperationState.Idle -> state.operation
            },
        )
        is TrashMutation.EntryActionStarted -> state.copy(
            operation = TrashOperationState.Entry(mutation.entryId),
        )
        TrashMutation.EntryActionFinished -> state.finishOperation()
        TrashMutation.EmptyStarted -> state.copy(operation = TrashOperationState.Emptying)
        TrashMutation.EmptyFinished -> state.finishOperation()
        is TrashMutation.ActionFailed -> state.copy(
            operation = TrashOperationState.Failed(mutation.message),
        )
    }
}

private fun TrashUiState.finishOperation(): TrashUiState = when (operation) {
    TrashOperationState.Emptying,
    is TrashOperationState.Entry -> copy(operation = TrashOperationState.Idle)
    is TrashOperationState.Failed,
    TrashOperationState.Idle -> this
}
