package com.aozijx.passly.presentation.feature.vault.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata

internal sealed interface DetailRevisionMutation {
    data class Opened(val entryId: EntryId) : DetailRevisionMutation
    data object Closed : DetailRevisionMutation
    data class MetadataChanged(
        val entryId: String,
        val revisions: List<EntryRevisionMetadata>,
    ) : DetailRevisionMutation
    data class ComparisonStarted(val entryId: String, val revisionId: String) : DetailRevisionMutation
    data class ComparisonLoaded(
        val entryId: String,
        val revisionId: String,
        val differences: List<EntryRevisionDifference>,
    ) : DetailRevisionMutation
    data class Failed(
        val entryId: String,
        val revisionId: String?,
        val failure: DetailRevisionFailure,
    ) : DetailRevisionMutation
    data object BackToList : DetailRevisionMutation
    data object RestoreRequested : DetailRevisionMutation
    data object RestoreCancelled : DetailRevisionMutation
    data object RestoreStarted : DetailRevisionMutation
    data object RestoreSucceeded : DetailRevisionMutation
}

internal object DetailRevisionReducer {
    fun reduce(
        state: DetailRevisionState,
        mutation: DetailRevisionMutation,
    ): DetailRevisionState = when (mutation) {
        is DetailRevisionMutation.Opened -> DetailRevisionState(
            visible = true,
            entryId = mutation.entryId,
            loading = true,
        )
        DetailRevisionMutation.Closed -> DetailRevisionState()
        is DetailRevisionMutation.MetadataChanged -> if (state.matches(mutation.entryId)) {
            state.copy(metadata = mutation.revisions, loading = false, failure = null)
        } else state
        is DetailRevisionMutation.ComparisonStarted -> if (state.matches(mutation.entryId)) {
            state.copy(
                destination = DetailRevisionDestination.COMPARISON,
                selectedRevisionId = mutation.revisionId,
                differences = emptyList(),
                loading = true,
                failure = null,
            )
        } else state
        is DetailRevisionMutation.ComparisonLoaded -> if (
            state.matches(mutation.entryId) && state.selectedRevisionId == mutation.revisionId
        ) {
            state.copy(differences = mutation.differences, loading = false, failure = null)
        } else state
        is DetailRevisionMutation.Failed -> if (
            state.matches(mutation.entryId) &&
            (mutation.revisionId == null || state.selectedRevisionId == mutation.revisionId)
        ) {
            state.copy(loading = false, restoring = false, failure = mutation.failure)
        } else state
        DetailRevisionMutation.BackToList -> state.copy(
            destination = DetailRevisionDestination.LIST,
            selectedRevisionId = null,
            differences = emptyList(),
            loading = false,
            confirmRestore = false,
            failure = null,
        )
        DetailRevisionMutation.RestoreRequested -> state.copy(confirmRestore = true)
        DetailRevisionMutation.RestoreCancelled -> state.copy(confirmRestore = false)
        DetailRevisionMutation.RestoreStarted -> state.copy(
            confirmRestore = false,
            restoring = true,
            failure = null,
        )
        DetailRevisionMutation.RestoreSucceeded -> state.copy(
            destination = DetailRevisionDestination.LIST,
            selectedRevisionId = null,
            differences = emptyList(),
            restoring = false,
            failure = null,
        )
    }

    private fun DetailRevisionState.matches(rawEntryId: String): Boolean =
        entryId?.value == rawEntryId
}
