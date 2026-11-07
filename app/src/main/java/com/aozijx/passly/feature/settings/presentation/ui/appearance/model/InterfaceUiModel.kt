package com.aozijx.passly.feature.settings.presentation.ui.appearance.model

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.EntryCardPresentation

data class InterfaceUiModel(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val appCornerRadiusDp: Float,
    val appCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
    val entryCardPresentations: List<EntryCardPresentation>,
)
