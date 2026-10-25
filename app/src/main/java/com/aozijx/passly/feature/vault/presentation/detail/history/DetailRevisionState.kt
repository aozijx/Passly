package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata

enum class DetailRevisionFailure { MISSING, STALE, INVALID, AUTHENTICATION, UNEXPECTED }

sealed interface DetailRevisionState {
    data object Closed : DetailRevisionState

    data class Open(
        val entryId: EntryId,
        val revisions: DetailRevisionListState = DetailRevisionListState.Loading,
        val page: DetailRevisionPage = DetailRevisionPage.List,
    ) : DetailRevisionState
}

sealed interface DetailRevisionListState {
    data object Loading : DetailRevisionListState
    data class Ready(val revisions: List<EntryRevisionMetadata>) : DetailRevisionListState
    data class Failed(val reason: DetailRevisionFailure) : DetailRevisionListState
}

sealed interface DetailRevisionPage {
    data object List : DetailRevisionPage
    data class Comparison(
        val revisionId: String,
        val phase: DetailRevisionComparisonPhase = DetailRevisionComparisonPhase.Loading,
    ) : DetailRevisionPage
}

sealed interface DetailRevisionComparisonPhase {
    data object Loading : DetailRevisionComparisonPhase
    data class Ready(
        val differences: List<EntryRevisionDifference>,
        val restore: DetailRevisionRestorePhase = DetailRevisionRestorePhase.Idle,
    ) : DetailRevisionComparisonPhase
    data class Failed(val reason: DetailRevisionFailure) : DetailRevisionComparisonPhase
}

enum class DetailRevisionRestorePhase {
    Idle,
    Confirming,
    Restoring,
}
