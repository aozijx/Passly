package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderInner
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Padding
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.sliderSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.VaultQuickFilter
import kotlin.math.roundToInt

@Composable
internal fun InterfaceDetail(
    state: InterfaceSettingsUiState,
    onStatusBarAutoHideChange: (Boolean) -> Unit,
    onTopBarCollapsibleChange: (Boolean) -> Unit,
    onQuickFilterBarCollapsibleChange: (Boolean) -> Unit,
    onOuterCornerRadiusChange: (Float) -> Unit,
    onInnerCornerRadiusChange: (Float) -> Unit,
    onGroupItemSpacingChange: (Float) -> Unit,
    onGroupContentPaddingChange: (Float) -> Unit,
    onVisibleVaultQuickFilterToggle: (VaultQuickFilter) -> Unit,
    onEntryHierarchyDisplayModeChange: (EntryHierarchyDisplayMode) -> Unit
) {
    var outerRadius by remember(state.outerCornerRadiusDp) {
        mutableFloatStateOf(state.outerCornerRadiusDp)
    }
    var innerRadius by remember(state.innerCornerRadiusDp) {
        mutableFloatStateOf(state.innerCornerRadiusDp)
    }
    var itemSpacing by remember(state.groupItemSpacingDp) {
        mutableFloatStateOf(state.groupItemSpacingDp)
    }
    var contentPadding by remember(state.groupContentPaddingDp) {
        mutableFloatStateOf(state.groupContentPaddingDp)
    }
    var showHierarchyModeMenu by remember { mutableStateOf(false) }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_interface_immersive_title))
        RoundedGroup(
            items = listOf(
                switchSettingsGroupItem(
                    key = "interface.status_bar_auto_hide",
                    icon = Icons.Default.Fullscreen,
                    title = stringResource(R.string.settings_interface_hide_status_bar),
                    subtitle = stringResource(R.string.settings_interface_hide_status_bar_description),
                    checked = state.hideSystemBars,
                    onCheckedChange = onStatusBarAutoHideChange
                ),
                switchSettingsGroupItem(
                    key = "interface.top_bar_collapsible",
                    icon = Icons.Default.ViewDay,
                    title = stringResource(R.string.settings_interface_top_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_top_bar_collapsible_description),
                    checked = state.collapseTopBarOnScroll,
                    onCheckedChange = onTopBarCollapsibleChange
                ),
                switchSettingsGroupItem(
                    key = "interface.quick_filter_bar_collapsible",
                    icon = Icons.Default.SpaceDashboard,
                    title = stringResource(R.string.settings_interface_quick_filter_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_quick_filter_bar_collapsible_description),
                    checked = state.collapseQuickFilterBarOnScroll,
                    onCheckedChange = onQuickFilterBarCollapsibleChange
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_interface_style_title))
        RoundedGroup(
            items = listOf(
                sliderSettingsGroupItem(
                    key = "interface.outer_corner_radius",
                    icon = Icons.Default.RoundedCorner,
                    title = stringResource(R.string.settings_interface_outer_corner_radius),
                    subtitle = stringResource(
                        R.string.settings_interface_outer_corner_radius_description
                    ),
                    value = outerRadius,
                    valueLabel = stringResource(
                        R.string.settings_value_dp,
                        outerRadius.roundToInt()
                    ),
                    valueRange =
                        InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP..
                                InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP,
                    steps = 47,
                    onValueChange = { outerRadius = it },
                    onValueChangeFinished = {
                        if (outerRadius != state.outerCornerRadiusDp) {
                            onOuterCornerRadiusChange(outerRadius)
                        }
                    }
                ),
                sliderSettingsGroupItem(
                    key = "interface.inner_corner_radius",
                    icon = Icons.Default.BorderInner,
                    title = stringResource(R.string.settings_interface_inner_corner_radius),
                    subtitle = stringResource(
                        R.string.settings_interface_inner_corner_radius_description
                    ),
                    value = innerRadius,
                    valueLabel = stringResource(
                        R.string.settings_value_dp,
                        innerRadius.roundToInt()
                    ),
                    valueRange =
                        InterfaceStyleConstraints.MIN_INNER_RADIUS_DP..
                                InterfaceStyleConstraints.MAX_INNER_RADIUS_DP,
                    steps = 23,
                    onValueChange = { innerRadius = it },
                    onValueChangeFinished = {
                        if (innerRadius != state.innerCornerRadiusDp) {
                            onInnerCornerRadiusChange(innerRadius)
                        }
                    }
                ),
                sliderSettingsGroupItem(
                    key = "interface.group_item_spacing",
                    icon = Icons.Default.FormatLineSpacing,
                    title = stringResource(R.string.settings_interface_group_item_spacing),
                    subtitle = stringResource(
                        R.string.settings_interface_group_item_spacing_description
                    ),
                    value = itemSpacing,
                    valueLabel = stringResource(
                        R.string.settings_value_dp,
                        itemSpacing.roundToInt()
                    ),
                    valueRange =
                        InterfaceStyleConstraints.MIN_ITEM_SPACING_DP..
                                InterfaceStyleConstraints.MAX_ITEM_SPACING_DP,
                    steps = 11,
                    onValueChange = { itemSpacing = it },
                    onValueChangeFinished = {
                        if (itemSpacing != state.groupItemSpacingDp) {
                            onGroupItemSpacingChange(itemSpacing)
                        }
                    }
                ),
                sliderSettingsGroupItem(
                    key = "interface.group_content_padding",
                    icon = Icons.Default.Padding,
                    title = stringResource(R.string.settings_interface_group_content_padding),
                    subtitle = stringResource(
                        R.string.settings_interface_group_content_padding_description
                    ),
                    value = contentPadding,
                    valueLabel = stringResource(
                        R.string.settings_value_dp,
                        contentPadding.roundToInt()
                    ),
                    valueRange =
                        InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP..
                                InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP,
                    steps = 23,
                    onValueChange = { contentPadding = it },
                    onValueChangeFinished = {
                        if (contentPadding != state.groupContentPaddingDp) {
                            onGroupContentPaddingChange(contentPadding)
                        }
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(
            text = stringResource(R.string.settings_interface_entry_hierarchy_section)
        )
        RoundedGroup(
            items = listOf(
                dropdownSettingsGroupItem(
                    key = "interface.entry_hierarchy_display_mode",
                    icon = Icons.Default.SpaceDashboard,
                    title = stringResource(
                        R.string.settings_interface_entry_hierarchy_display_mode
                    ),
                    selected = state.entryHierarchyDisplayMode,
                    selectedLabel = stringResource(
                        state.entryHierarchyDisplayMode.labelResource()
                    ),
                    options = EntryHierarchyDisplayMode.entries.map {
                        it to stringResource(it.labelResource())
                    },
                    expanded = showHierarchyModeMenu,
                    onExpandedChange = { showHierarchyModeMenu = it },
                    onSelect = onEntryHierarchyDisplayModeChange
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        VaultQuickFiltersSettingsSection(
            enabledVaultQuickFilterKeys = state.enabledVaultQuickFilterKeys,
            onVaultQuickFilterToggle = onVisibleVaultQuickFilterToggle
        )
    }
}

private fun EntryHierarchyDisplayMode.labelResource(): Int = when (this) {
    EntryHierarchyDisplayMode.COLLAPSED ->
        R.string.settings_interface_entry_hierarchy_collapsed

    EntryHierarchyDisplayMode.EXPANDED ->
        R.string.settings_interface_entry_hierarchy_expanded

    EntryHierarchyDisplayMode.SEPARATE ->
        R.string.settings_interface_entry_hierarchy_separate
}
