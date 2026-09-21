package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.InterfaceUiModel

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
