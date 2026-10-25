package com.aozijx.passly.feature.settings.presentation.appearance

import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.feature.settings.presentation.ui.appearance.model.InterfaceUiModel

fun InterfaceSettingsUiState.Ready.toInterfaceUiModel(): InterfaceUiModel = InterfaceUiModel(
    hideSystemBars = hideSystemBars,
    collapseTopBarOnScroll = collapseTopBarOnScroll,
    collapseQuickFilterBarOnScroll = collapseQuickFilterBarOnScroll,
    appCornerRadiusDp = appCornerRadiusDp,
    appCornerRadiusRange =
        AppCornerRadiusConstraints.MIN_DP..AppCornerRadiusConstraints.MAX_DP,
    entryHierarchyDisplayMode = entryHierarchyDisplayMode,
)
