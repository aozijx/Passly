package com.aozijx.passly.presentation.ui.settings.appearance.model

data class InterfaceUiModel(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val outerCornerRadiusDp: Float,
    val outerCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val innerCornerRadiusDp: Float,
    val innerCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val groupItemSpacingDp: Float,
    val groupItemSpacingRange: ClosedFloatingPointRange<Float>,
    val groupContentPaddingDp: Float,
    val groupContentPaddingRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayModeUiModel,
)

enum class EntryHierarchyDisplayModeUiModel {
    COLLAPSED,
    EXPANDED,
    SEPARATE,
}
