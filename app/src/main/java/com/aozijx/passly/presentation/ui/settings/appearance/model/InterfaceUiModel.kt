package com.aozijx.passly.presentation.ui.settings.appearance.model

data class InterfaceUiModel(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val appCornerRadiusDp: Float,
    val appCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayModeUiModel,
)

interface InterfaceEventHandler {
    fun onStatusBarAutoHideChanged(enabled: Boolean)
    fun onTopBarCollapsibleChanged(enabled: Boolean)
    fun onQuickFilterBarCollapsibleChanged(enabled: Boolean)
    fun onAppCornerRadiusChanged(radiusDp: Float)
    fun onEntryHierarchyDisplayModeChanged(mode: EntryHierarchyDisplayModeUiModel)
}

enum class EntryHierarchyDisplayModeUiModel {
    COLLAPSED,
    EXPANDED,
    SEPARATE,
}
