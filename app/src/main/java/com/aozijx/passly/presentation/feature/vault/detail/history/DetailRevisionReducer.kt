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
        is DetailRevisionMutation.Opened -> DetailRevisionState.Open(mutation.entryId)
        DetailRevisionMutation.Closed -> DetailRevisionState.Closed
        is DetailRevisionMutation.MetadataChanged -> state.updateOpen(mutation.entryId) {
            copy(revisions = DetailRevisionListState.Ready(mutation.revisions))
        }
        is DetailRevisionMutation.ComparisonStarted -> state.updateOpen(mutation.entryId) {
            copy(page = DetailRevisionPage.Comparison(mutation.revisionId))
        }
        is DetailRevisionMutation.ComparisonLoaded -> state.updateComparison(
            entryId = mutation.entryId,
            revisionId = mutation.revisionId,
        ) {
            copy(phase = DetailRevisionComparisonPhase.Ready(mutation.differences))
        }
        is DetailRevisionMutation.Failed -> {
            if (mutation.revisionId == null) {
                state.updateOpen(mutation.entryId) {
                    copy(revisions = DetailRevisionListState.Failed(mutation.failure))
                }
            } else {
                state.updateComparison(mutation.entryId, mutation.revisionId) {
                    copy(phase = DetailRevisionComparisonPhase.Failed(mutation.failure))
                }
            }
        }
        DetailRevisionMutation.BackToList -> state.updateOpen { copy(page = DetailRevisionPage.List) }
        DetailRevisionMutation.RestoreRequested -> state.updateReadyComparison {
            copy(restore = DetailRevisionRestorePhase.Confirming)
        }
        DetailRevisionMutation.RestoreCancelled -> state.updateReadyComparison {
            copy(restore = DetailRevisionRestorePhase.Idle)
        }
        DetailRevisionMutation.RestoreStarted -> state.updateReadyComparison {
            copy(restore = DetailRevisionRestorePhase.Restoring)
        }
        DetailRevisionMutation.RestoreSucceeded -> state.updateOpen {
            copy(page = DetailRevisionPage.List)
        }
    }
}

private inline fun DetailRevisionState.updateOpen(
    entryId: String? = null,
    transform: DetailRevisionState.Open.() -> DetailRevisionState.Open,
): DetailRevisionState {
    val open = this as? DetailRevisionState.Open ?: return this
    if (entryId != null && open.entryId.value != entryId) return this
    return open.transform()
}

private inline fun DetailRevisionState.updateComparison(
    entryId: String,
    revisionId: String,
    transform: DetailRevisionPage.Comparison.() -> DetailRevisionPage.Comparison,
): DetailRevisionState = updateOpen(entryId) {
    val comparison = page as? DetailRevisionPage.Comparison ?: return@updateOpen this
    if (comparison.revisionId != revisionId) return@updateOpen this
    copy(page = comparison.transform())
}

private inline fun DetailRevisionState.updateReadyComparison(
    transform: DetailRevisionComparisonPhase.Ready.() -> DetailRevisionComparisonPhase.Ready,
): DetailRevisionState = updateOpen {
    val comparison = page as? DetailRevisionPage.Comparison ?: return@updateOpen this
    val ready = comparison.phase as? DetailRevisionComparisonPhase.Ready ?: return@updateOpen this
    copy(page = comparison.copy(phase = ready.transform()))
}
