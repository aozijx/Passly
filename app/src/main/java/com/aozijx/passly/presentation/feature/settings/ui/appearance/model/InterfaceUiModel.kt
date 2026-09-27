package com.aozijx.passly.presentation.feature.settings.ui.appearance.model

data class InterfaceUiModel(
    val hideSystemBars: Boolean,
    val collapseTopBarOnScroll: Boolean,
    val collapseQuickFilterBarOnScroll: Boolean,
    val appCornerRadiusDp: Float,
    val appCornerRadiusRange: ClosedFloatingPointRange<Float>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayModeUiModel,
)

sealed interface InterfaceSettingsEvent {
    data class StatusBarAutoHideChanged(val enabled: Boolean) : InterfaceSettingsEvent
    data class TopBarCollapsibleChanged(val enabled: Boolean) : InterfaceSettingsEvent
    data class QuickFilterBarCollapsibleChanged(val enabled: Boolean) : InterfaceSettingsEvent
    data class AppCornerRadiusChanged(val radiusDp: Float) : InterfaceSettingsEvent
    data class EntryHierarchyDisplayModeChanged(
        val mode: EntryHierarchyDisplayModeUiModel,
    ) : InterfaceSettingsEvent
}

enum class EntryHierarchyDisplayModeUiModel {
    COLLAPSED,
    EXPANDED,
    SEPARATE,
}
