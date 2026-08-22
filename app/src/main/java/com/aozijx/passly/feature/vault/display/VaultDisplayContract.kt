package com.aozijx.passly.feature.vault.display

import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.SwipeActionType

data class VaultLayoutConfig(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseQuickFilterBarOnScroll: Boolean = false,
)

data class VaultStyleConfig(
    val entryCardPresentations: List<EntryCardPresentation> = emptyList(),
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED,
)

data class VaultInteractionConfig(
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
)

data class VaultDisplayUiState(
    val layout: VaultLayoutConfig = VaultLayoutConfig(),
    val style: VaultStyleConfig = VaultStyleConfig(),
    val interaction: VaultInteractionConfig = VaultInteractionConfig()
)
