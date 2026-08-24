package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.presentation.ui.settings.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.InterfaceUiModel

fun InterfaceSettingsUiState.toInterfaceUiModel(): InterfaceUiModel = InterfaceUiModel(
    hideSystemBars = hideSystemBars,
    collapseTopBarOnScroll = collapseTopBarOnScroll,
    collapseQuickFilterBarOnScroll = collapseQuickFilterBarOnScroll,
    outerCornerRadiusDp = outerCornerRadiusDp,
    outerCornerRadiusRange = InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP..
        InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP,
    innerCornerRadiusDp = innerCornerRadiusDp,
    innerCornerRadiusRange = InterfaceStyleConstraints.MIN_INNER_RADIUS_DP..
        InterfaceStyleConstraints.MAX_INNER_RADIUS_DP,
    groupItemSpacingDp = groupItemSpacingDp,
    groupItemSpacingRange = InterfaceStyleConstraints.MIN_ITEM_SPACING_DP..
        InterfaceStyleConstraints.MAX_ITEM_SPACING_DP,
    groupContentPaddingDp = groupContentPaddingDp,
    groupContentPaddingRange = InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP..
        InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP,
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
