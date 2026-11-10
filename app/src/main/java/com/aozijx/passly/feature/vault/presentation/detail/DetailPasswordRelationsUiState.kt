package com.aozijx.passly.feature.vault.presentation.detail

sealed interface RelatedPasswordBindingUiState {
    data object Closed : RelatedPasswordBindingUiState

    data object Loading : RelatedPasswordBindingUiState

    data object LoadFailed : RelatedPasswordBindingUiState

    data class Ready(
        val candidates: List<RelatedPasswordCandidateUiModel>,
        val query: String = "",
        val operation: RelatedPasswordBindingOperationState = RelatedPasswordBindingOperationState.Idle,
    ) : RelatedPasswordBindingUiState {
        val results: RelatedPasswordCandidateSearchResults
            get() = candidates.toSearchResults(query)
    }
}

sealed interface RelatedPasswordBindingOperationState {
    data object Idle : RelatedPasswordBindingOperationState

    data class Saving(val entryId: String) : RelatedPasswordBindingOperationState

    data object Failed : RelatedPasswordBindingOperationState
}

sealed interface RelatedPasswordCandidateSearchResults {
    data object NoCandidates : RelatedPasswordCandidateSearchResults

    data object NoMatches : RelatedPasswordCandidateSearchResults

    data class Matches(
        val candidates: List<RelatedPasswordCandidateUiModel>,
    ) : RelatedPasswordCandidateSearchResults
}

internal fun List<RelatedPasswordCandidateUiModel>.toSearchResults(query: String): RelatedPasswordCandidateSearchResults {
    if (isEmpty()) return RelatedPasswordCandidateSearchResults.NoCandidates
    val matches =
        if (query.isBlank()) {
            this
        } else {
            filter { candidate ->
                candidate.title.contains(query, ignoreCase = true) ||
                    candidate.username.contains(query, ignoreCase = true)
            }
        }
    return if (matches.isEmpty()) {
        RelatedPasswordCandidateSearchResults.NoMatches
    } else {
        RelatedPasswordCandidateSearchResults.Matches(matches)
    }
}

data class RelatedPasswordCandidateUiModel(
    val id: String,
    val title: String,
    val username: String,
)

sealed interface RelatedPasswordUnlinkUiState {
    data object Closed : RelatedPasswordUnlinkUiState

    data class AwaitingConfirmation(
        val target: RelatedPasswordUnlinkTargetUiModel,
    ) : RelatedPasswordUnlinkUiState

    data class Removing(
        val target: RelatedPasswordUnlinkTargetUiModel,
    ) : RelatedPasswordUnlinkUiState

    data class Failed(
        val target: RelatedPasswordUnlinkTargetUiModel,
    ) : RelatedPasswordUnlinkUiState
}

data class RelatedPasswordUnlinkTargetUiModel(
    val targetEntryId: String,
    val linkId: String,
    val targetTitle: String,
)
