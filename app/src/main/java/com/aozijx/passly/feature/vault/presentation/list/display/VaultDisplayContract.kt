package com.aozijx.passly.feature.vault.presentation.list.display

import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.SwipeActionType

data class VaultLayoutConfig(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
)

data class VaultStyleConfig(
    val entryCardPresentations: List<EntryCardPresentation>,
)

data class VaultInteractionConfig(
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
    val swipeLeftAction: SwipeActionType,
    val swipeRightAction: SwipeActionType,
)

sealed interface VaultDisplayUiState {
    data object Loading : VaultDisplayUiState

    data class Ready(
        val layout: VaultLayoutConfig,
        val style: VaultStyleConfig,
        val interaction: VaultInteractionConfig,
    ) : VaultDisplayUiState
}
