package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.presentation.ui.settings.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.InterfaceUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.LibraryQuickFilterOptionUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.LibraryQuickFilterUiModel

fun InterfaceSettingsUiState.toInterfaceUiModel(): InterfaceUiModel = InterfaceUiModel(
    hideSystemBars = hideSystemBars,
    collapseTopBarOnScroll = collapseTopBarOnScroll,
    collapseQuickFilterBarOnScroll = collapseQuickFilterBarOnScroll,
    appCornerRadiusDp = appCornerRadiusDp,
    appCornerRadiusRange =
        AppCornerRadiusConstraints.MIN_DP..AppCornerRadiusConstraints.MAX_DP,
    entryHierarchyDisplayMode = entryHierarchyDisplayMode.toUiModel(),
)

fun EntryHierarchyDisplayMode.toUiModel(): EntryHierarchyDisplayModeUiModel = when (this) {
    EntryHierarchyDisplayMode.COLLAPSED -> EntryHierarchyDisplayModeUiModel.COLLAPSED
    EntryHierarchyDisplayMode.EXPANDED -> EntryHierarchyDisplayModeUiModel.EXPANDED
    EntryHierarchyDisplayMode.SEPARATE -> EntryHierarchyDisplayModeUiModel.SEPARATE
}

fun EntryHierarchyDisplayModeUiModel.toDomainModel(): EntryHierarchyDisplayMode = when (this) {
    EntryHierarchyDisplayModeUiModel.COLLAPSED -> EntryHierarchyDisplayMode.COLLAPSED
    EntryHierarchyDisplayModeUiModel.EXPANDED -> EntryHierarchyDisplayMode.EXPANDED
    EntryHierarchyDisplayModeUiModel.SEPARATE -> EntryHierarchyDisplayMode.SEPARATE
}

fun libraryQuickFilterOptions(
    enabledKeys: Set<String>,
): List<LibraryQuickFilterOptionUiModel> = LibraryQuickFilter.toggleableVisibleQuickFilters.map {
    LibraryQuickFilterOptionUiModel(
        filter = it.toUiModel(),
        selected = it.settingsKey in enabledKeys,
    )
}

fun LibraryQuickFilterUiModel.toDomainModel(): LibraryQuickFilter = when (this) {
    LibraryQuickFilterUiModel.PASSWORDS -> LibraryQuickFilter.PASSWORDS
    LibraryQuickFilterUiModel.TOTP -> LibraryQuickFilter.TOTP
}

private fun LibraryQuickFilter.toUiModel(): LibraryQuickFilterUiModel = when (this) {
    LibraryQuickFilter.PASSWORDS -> LibraryQuickFilterUiModel.PASSWORDS
    LibraryQuickFilter.TOTP -> LibraryQuickFilterUiModel.TOTP
    LibraryQuickFilter.ALL -> error("ALL is not a toggleable quick filter")
}
