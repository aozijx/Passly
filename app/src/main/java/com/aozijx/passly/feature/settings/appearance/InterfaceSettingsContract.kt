package com.aozijx.passly.feature.settings.appearance

import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints

data class InterfaceSettingsUiState(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false,
    val outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    val innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    val groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    val groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED,
)

sealed interface InterfaceSettingsAction {
    data class SetHideSystemBars(val enabled: Boolean) : InterfaceSettingsAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetTabBarCollapsible(val enabled: Boolean) : InterfaceSettingsAction
    data class SetOuterCornerRadius(val radiusDp: Float) : InterfaceSettingsAction
    data class SetInnerCornerRadius(val radiusDp: Float) : InterfaceSettingsAction
    data class SetGroupItemSpacing(val spacingDp: Float) : InterfaceSettingsAction
    data class SetGroupContentPadding(val paddingDp: Float) : InterfaceSettingsAction
    data class SetVisibleVaultTabs(val tabs: Set<String>) : InterfaceSettingsAction
    data class SetMaxTabsWithoutScroll(val maxTabs: Int) : InterfaceSettingsAction
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : InterfaceSettingsAction
}
