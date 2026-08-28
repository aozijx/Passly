package com.aozijx.passly.presentation.ui.settings.appearance

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
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
import com.aozijx.passly.presentation.ui.shared.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.ui.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.InterfaceEventHandler
import com.aozijx.passly.presentation.ui.settings.appearance.model.InterfaceUiModel
import kotlin.math.roundToInt

@Composable
internal fun InterfaceDetail(
    state: InterfaceUiModel,
    eventHandler: InterfaceEventHandler,
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
                    onCheckedChange = eventHandler::onStatusBarAutoHideChanged,
                ),
                switchSettingsGroupItem(
                    key = "interface.top_bar_collapsible",
                    icon = Icons.Default.ViewDay,
                    title = stringResource(R.string.settings_interface_top_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_top_bar_collapsible_description),
                    checked = state.collapseTopBarOnScroll,
                    onCheckedChange = eventHandler::onTopBarCollapsibleChanged,
                ),
                switchSettingsGroupItem(
                    key = "interface.quick_filter_bar_collapsible",
                    icon = Icons.Default.SpaceDashboard,
                    title = stringResource(R.string.settings_interface_quick_filter_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_quick_filter_bar_collapsible_description),
                    checked = state.collapseQuickFilterBarOnScroll,
                    onCheckedChange = eventHandler::onQuickFilterBarCollapsibleChanged,
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
                        state.outerCornerRadiusRange,
                    steps = 47,
                    onValueChange = { outerRadius = it },
                    onValueChangeFinished = {
                        if (outerRadius != state.outerCornerRadiusDp) {
                            eventHandler.onOuterCornerRadiusChanged(outerRadius)
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
                        state.innerCornerRadiusRange,
                    steps = 23,
                    onValueChange = { innerRadius = it },
                    onValueChangeFinished = {
                        if (innerRadius != state.innerCornerRadiusDp) {
                            eventHandler.onInnerCornerRadiusChanged(innerRadius)
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
                        state.groupItemSpacingRange,
                    steps = 11,
                    onValueChange = { itemSpacing = it },
                    onValueChangeFinished = {
                        if (itemSpacing != state.groupItemSpacingDp) {
                            eventHandler.onGroupItemSpacingChanged(itemSpacing)
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
                        state.groupContentPaddingRange,
                    steps = 23,
                    onValueChange = { contentPadding = it },
                    onValueChangeFinished = {
                        if (contentPadding != state.groupContentPaddingDp) {
                            eventHandler.onGroupContentPaddingChanged(contentPadding)
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
                    options = EntryHierarchyDisplayModeUiModel.entries.map {
                        it to stringResource(it.labelResource())
                    },
                    expanded = showHierarchyModeMenu,
                    onExpandedChange = { showHierarchyModeMenu = it },
                    onSelect = eventHandler::onEntryHierarchyDisplayModeChanged,
                )
            )
        )

    }
}

private fun EntryHierarchyDisplayModeUiModel.labelResource(): Int = when (this) {
    EntryHierarchyDisplayModeUiModel.COLLAPSED ->
        R.string.settings_interface_entry_hierarchy_collapsed

    EntryHierarchyDisplayModeUiModel.EXPANDED ->
        R.string.settings_interface_entry_hierarchy_expanded

    EntryHierarchyDisplayModeUiModel.SEPARATE ->
        R.string.settings_interface_entry_hierarchy_separate
}
