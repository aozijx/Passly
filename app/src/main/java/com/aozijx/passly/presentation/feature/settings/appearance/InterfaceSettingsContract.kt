package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter

data class InterfaceSettingsUiState(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseQuickFilterBarOnScroll: Boolean = false,
    val appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    val enabledLibraryQuickFilterKeys: Set<String> = LibraryQuickFilter.defaultVisibleKeys,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED,
)

sealed interface InterfaceSettingsAction {
    data class SetHideSystemBars(val enabled: Boolean) : InterfaceSettingsAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetQuickFilterBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetAppCornerRadius(val radiusDp: Float) : InterfaceSettingsAction
    data class ToggleVisibleLibraryQuickFilter(
        val quickFilter: LibraryQuickFilter
    ) : InterfaceSettingsAction
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : InterfaceSettingsAction
}
