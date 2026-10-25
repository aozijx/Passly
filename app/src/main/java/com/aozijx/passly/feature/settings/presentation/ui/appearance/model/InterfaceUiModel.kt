package com.aozijx.passly.feature.settings.presentation.ui.appearance.model

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode

data class InterfaceUiModel(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val appCornerRadiusDp: Float,
    val appCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
)
