package com.aozijx.passly.feature.settings.presentation.ui.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
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
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.dropdownSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsAction
import com.aozijx.passly.feature.settings.presentation.ui.appearance.model.InterfaceUiModel
import kotlin.math.roundToInt

@Composable
internal fun InterfaceDetail(
    state: InterfaceUiModel,
    onAction: (InterfaceSettingsAction) -> Unit,
) {
    var appCornerRadius by remember(state.appCornerRadiusDp) {
        mutableFloatStateOf(state.appCornerRadiusDp)
    }
    var showHierarchyModeMenu by remember { mutableStateOf(false) }

    SettingsSection {
        SettingsSectionTitle(text = stringResource(R.string.settings_interface_immersive_title))
        SegmentedSettingsGroup(
            items = listOf(
                switchSettingsGroupItem(
                    key = "interface.status_bar_auto_hide",
                    icon = Icons.Default.Fullscreen,
                    title = stringResource(R.string.settings_interface_hide_status_bar),
                    subtitle = stringResource(R.string.settings_interface_hide_status_bar_description),
                    checked = state.hideSystemBars,
                    onCheckedChange = { enabled ->
                        onAction(InterfaceSettingsAction.SetHideSystemBars(enabled))
                    },
                ),
                switchSettingsGroupItem(
                    key = "interface.top_bar_collapsible",
                    icon = Icons.Default.ViewDay,
                    title = stringResource(R.string.settings_interface_top_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_top_bar_collapsible_description),
                    checked = state.collapseTopBarOnScroll,
                    onCheckedChange = { enabled ->
                        onAction(InterfaceSettingsAction.SetTopBarCollapsible(enabled))
                    },
                ),
                switchSettingsGroupItem(
                    key = "interface.quick_filter_bar_collapsible",
                    icon = Icons.Default.SpaceDashboard,
                    title = stringResource(R.string.settings_interface_quick_filter_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_quick_filter_bar_collapsible_description),
                    checked = state.collapseQuickFilterBarOnScroll,
                    onCheckedChange = { enabled ->
                        onAction(InterfaceSettingsAction.SetQuickFilterBarCollapsible(enabled))
                    },
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_interface_shape_title))
        SegmentedSettingsGroup(
            items = listOf(
                sliderSettingsGroupItem(
                    key = "interface.app_corner_radius",
                    icon = Icons.Default.RoundedCorner,
                    title = stringResource(R.string.settings_interface_app_corner_radius),
                    subtitle = stringResource(
                        R.string.settings_interface_app_corner_radius_description
                    ),
                    value = appCornerRadius,
                    valueLabel = stringResource(
                        R.string.settings_value_dp,
                        appCornerRadius.roundToInt()
                    ),
                    valueRange = state.appCornerRadiusRange,
                    steps = 47,
                    onValueChange = { appCornerRadius = it },
                    onValueChangeFinished = {
                        if (appCornerRadius != state.appCornerRadiusDp) {
                            onAction(
                                InterfaceSettingsAction.SetAppCornerRadius(appCornerRadius),
                            )
                        }
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(
            text = stringResource(R.string.settings_interface_entry_hierarchy_section)
        )
        SegmentedSettingsGroup(
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
                    onSelect = { mode ->
                        onAction(InterfaceSettingsAction.SetEntryHierarchyDisplayMode(mode))
                    },
                )
            )
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
