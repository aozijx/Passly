package com.aozijx.passly.presentation.feature.vault.trash

import com.aozijx.passly.domain.entry.model.query.EntryListItem

internal sealed interface TrashMutation {
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
        is TrashMutation.Loaded -> state.copy(entries = mutation.entries, isLoading = false)
        is TrashMutation.LoadFailed -> state.copy(isLoading = false, error = mutation.message)
        TrashMutation.ErrorCleared -> state.copy(error = null)
        is TrashMutation.EntryActionStarted -> state.copy(activeEntryId = mutation.entryId, error = null)
        TrashMutation.EntryActionFinished -> state.copy(activeEntryId = null)
        TrashMutation.EmptyStarted -> state.copy(isEmptying = true, error = null)
        TrashMutation.EmptyFinished -> state.copy(isEmptying = false)
        is TrashMutation.ActionFailed -> state.copy(error = mutation.message)
    }
}
