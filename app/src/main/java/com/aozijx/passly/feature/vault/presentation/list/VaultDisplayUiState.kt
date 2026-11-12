package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.SwipeActionType

sealed interface VaultDisplayUiState {
    data object Loading : VaultDisplayUiState

    data object Failed : VaultDisplayUiState

    data class Ready(
        val hideSystemBars: Boolean,
        val collapseTopBarOnScroll: Boolean,
        val collapseQuickFilterBarOnScroll: Boolean,
        val entryCardPresentations: List<EntryCardPresentation>,
        val isSwipeEnabled: Boolean,
        val isPullToSearchEnabled: Boolean,
        val swipeLeftAction: SwipeActionType,
        val swipeRightAction: SwipeActionType,
        val sort: EntrySort,
    ) : VaultDisplayUiState
}
