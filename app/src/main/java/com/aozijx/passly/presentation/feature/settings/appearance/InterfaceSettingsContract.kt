package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
sealed interface InterfaceSettingsUiState {
    data object Loading : InterfaceSettingsUiState

    data class Ready(
        val hideSystemBars: Boolean,
        val collapseTopBarOnScroll: Boolean,
        val collapseQuickFilterBarOnScroll: Boolean,
        val appCornerRadiusDp: Float,
        val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
    ) : InterfaceSettingsUiState
}

sealed interface InterfaceSettingsAction {
    data class SetHideSystemBars(val enabled: Boolean) : InterfaceSettingsAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetQuickFilterBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetAppCornerRadius(val radiusDp: Float) : InterfaceSettingsAction
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : InterfaceSettingsAction
}
