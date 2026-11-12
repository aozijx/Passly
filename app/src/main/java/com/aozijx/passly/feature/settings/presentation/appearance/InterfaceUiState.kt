package com.aozijx.passly.feature.settings.presentation.appearance

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.EntryCardPresentation

data class InterfaceUiState(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val appCornerRadiusDp: Float,
    val appCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
    val entryHierarchyDisplayModeLabelRes: Int,
    val entryHierarchyDisplayModeOptions: List<EntryHierarchyDisplayModeOptionUiState>,
    val cardStylePresentations: List<EntryCardPresentation>,
    val copyNextTotpNearExpiry: Boolean,
)

data class EntryHierarchyDisplayModeOptionUiState(
    val mode: EntryHierarchyDisplayMode,
    val labelRes: Int,
)

data class CardStyleGroupUiState(
    val entryType: EntryType,
    val entryTypeKey: String,
    val options: List<CardStyleOptionUiState>,
)

data class CardStyleOptionUiState(
    val styleKey: String,
    val titleRes: Int,
    val selected: Boolean,
    val selection: EntryCardPresentation,
)
