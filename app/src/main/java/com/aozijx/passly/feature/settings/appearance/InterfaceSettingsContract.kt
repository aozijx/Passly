package com.aozijx.passly.feature.settings.appearance

import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.feature.vault.model.VaultTab

data class InterfaceSettingsUiState(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false,
    val outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    val innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    val groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    val groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP,
    val enabledVaultTabKeys: Set<String> = VaultTab.defaultVisibleKeys,
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
    data class ToggleVisibleVaultTab(val tab: VaultTab) : InterfaceSettingsAction
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : InterfaceSettingsAction
}
