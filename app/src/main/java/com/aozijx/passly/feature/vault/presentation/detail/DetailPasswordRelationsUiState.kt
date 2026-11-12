package com.aozijx.passly.feature.vault.presentation.detail

sealed interface RelatedPasswordBindingUiState {
    val query: String
        get() = ""

    data object Closed : RelatedPasswordBindingUiState

    data object Loading : RelatedPasswordBindingUiState

    data object LoadFailed : RelatedPasswordBindingUiState

    data class Ready(
        val candidates: List<RelatedPasswordCandidateUiModel>,
        override val query: String = "",
        val operation: RelatedPasswordBindingOperationState = RelatedPasswordBindingOperationState.Idle,
    ) : RelatedPasswordBindingUiState {
        val results: RelatedPasswordCandidateSearchResults
            get() = candidates.toSearchResults(query)
    }
}

sealed interface RelatedPasswordBindingOperationState {
    data object Idle : RelatedPasswordBindingOperationState

    data class Saving(
        val entryId: String,
    ) : RelatedPasswordBindingOperationState

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
                    candidate.username.matches(query)
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
    val username: RelatedPasswordCandidateUsernameUiState,
)

sealed interface RelatedPasswordCandidateUsernameUiState {
    data object Hidden : RelatedPasswordCandidateUsernameUiState

    data class Visible(
        val value: String,
    ) : RelatedPasswordCandidateUsernameUiState
}

private fun RelatedPasswordCandidateUsernameUiState.matches(query: String): Boolean =
    when (this) {
        RelatedPasswordCandidateUsernameUiState.Hidden -> false
        is RelatedPasswordCandidateUsernameUiState.Visible -> value.contains(query, ignoreCase = true)
    }

sealed interface RelatedPasswordUnlinkUiState {
    data object Closed : RelatedPasswordUnlinkUiState

    data class Open(
        val target: RelatedPasswordUnlinkTargetUiModel,
        val phase: RelatedPasswordUnlinkPhase,
    ) : RelatedPasswordUnlinkUiState
}

enum class RelatedPasswordUnlinkPhase { AwaitingConfirmation, Removing, Failed }

data class RelatedPasswordUnlinkTargetUiModel(
    val targetEntryId: String,
    val linkId: String,
    val targetTitle: String,
)
