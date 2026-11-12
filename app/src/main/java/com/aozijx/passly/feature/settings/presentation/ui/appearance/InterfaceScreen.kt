package com.aozijx.passly.feature.settings.presentation.ui.appearance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.feature.settings.presentation.appearance.CardStyleGroupUiState
import com.aozijx.passly.feature.settings.presentation.appearance.CardStyleOptionUiState
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsAction
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.component.cardstyle.CardStyleRegistry
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.anchoredSelectionSettingsItem
import com.aozijx.passly.presentation.shared.components.group.model.SegmentedSettingsItem
import com.aozijx.passly.presentation.shared.components.group.settingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.settingsSegmentedColors
import com.aozijx.passly.presentation.shared.components.group.sliderSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.presentation.shared.entry.labelRes
import kotlin.math.roundToInt

@Composable
internal fun InterfaceDetail(
    state: InterfaceUiState,
    onAction: (InterfaceSettingsAction) -> Unit,
) {
    val cardStyleGroups =
        remember(state.cardStylePresentations) {
            interfaceCardStyleGroups(state.cardStylePresentations)
        }
    var appCornerRadius by remember(state.appCornerRadiusDp) {
        mutableFloatStateOf(state.appCornerRadiusDp)
    }
    var showHierarchyModeMenu by remember { mutableStateOf(false) }

    SettingsSection {
        SettingsSectionTitle(text = stringResource(R.string.settings_interface_immersive_title))
        SegmentedSettingsGroup(
            items =
                listOf(
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
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        var cardStylesExpanded by rememberSaveable { mutableStateOf(false) }
        SegmentedSettingsGroup(
            items =
                listOf(
                    settingsGroupItem(
                        key = "interface.card_style.header",
                        title = stringResource(R.string.settings_interface_card_style_section),
                        onClick = { cardStylesExpanded = !cardStylesExpanded },
                        trailing = {
                            Icon(
                                imageVector = if (cardStylesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = stringResource(R.string.settings_interface_card_style_section_toggle),
                            )
                        },
                    ),
                    switchSettingsGroupItem(
                        key = "interface.copy_next_totp_near_expiry",
                        visible = cardStylesExpanded,
                        icon = Icons.Default.ContentCopy,
                        title = stringResource(R.string.settings_interface_copy_next_totp_near_expiry),
                        subtitle = stringResource(R.string.settings_interface_copy_next_totp_near_expiry_description),
                        checked = state.copyNextTotpNearExpiry,
                        onCheckedChange = { enabled ->
                            onAction(InterfaceSettingsAction.SetCopyNextTotpNearExpiry(enabled))
                        },
                    ),
                    SegmentedSettingsItem(
                        key = "interface.card_style.options",
                        visible = cardStylesExpanded,
                    ) { shapes ->
                        SegmentedListItem(
                            shapes = shapes,
                            colors = settingsSegmentedColors(),
                            contentPadding = PaddingValues(12.dp),
                            content = {
                                Column {
                                    cardStyleGroups.forEachIndexed { index, group ->
                                        if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                                        CardStyleChoices(
                                            group = group,
                                            onSelect = { selection ->
                                                onAction(InterfaceSettingsAction.SetEntryCardPresentation(selection))
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    },
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_interface_shape_title))
        SegmentedSettingsGroup(
            items =
                listOf(
                    sliderSettingsGroupItem(
                        key = "interface.app_corner_radius",
                        icon = Icons.Default.RoundedCorner,
                        title = stringResource(R.string.settings_interface_app_corner_radius),
                        subtitle =
                            stringResource(
                                R.string.settings_interface_app_corner_radius_description,
                            ),
                        value = appCornerRadius,
                        valueLabel =
                            stringResource(
                                R.string.settings_value_dp,
                                appCornerRadius.roundToInt(),
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
                        },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(
            text = stringResource(R.string.settings_interface_entry_hierarchy_section),
        )
        SegmentedSettingsGroup(
            items =
                listOf(
                    anchoredSelectionSettingsItem(
                        key = "interface.entry_hierarchy_display_mode",
                        icon = Icons.Default.SpaceDashboard,
                        title =
                            stringResource(
                                R.string.settings_interface_entry_hierarchy_display_mode,
                            ),
                        selected = state.entryHierarchyDisplayMode,
                        selectedLabel = stringResource(state.entryHierarchyDisplayModeLabelRes),
                        options =
                            state.entryHierarchyDisplayModeOptions.map { option ->
                                option.mode to stringResource(option.labelRes)
                            },
                        expanded = showHierarchyModeMenu,
                        onExpandedChange = { showHierarchyModeMenu = it },
                        onSelect = { mode ->
                            onAction(InterfaceSettingsAction.SetEntryHierarchyDisplayMode(mode))
                        },
                    ),
                ),
        )
    }
}

internal fun interfaceCardStyleGroups(presentations: List<EntryCardPresentation>): List<CardStyleGroupUiState> =
    CardStyleRegistry.configurableEntryTypes.map { entryType ->
        val entryTypeKey = CardStyleRegistry.entryTypeKey(entryType)
        val storedPresentation = presentations.firstOrNull { it.entryTypeKey == entryTypeKey }
        val styles = CardStyleRegistry.stylesFor(entryType)
        val selectedStyle = CardStyleRegistry.configuredStyleFor(entryType, storedPresentation?.variantKey)
        val basePresentation =
            storedPresentation
                ?: EntryCardPresentation(
                    entryTypeKey = entryTypeKey,
                    variantKey = selectedStyle.key,
                    density = CardDensity.STANDARD,
                    showIcon = true,
                    showFavorite = true,
                    showSecondaryText = true,
                    showQuickAction = true,
                )

        CardStyleGroupUiState(
            entryType = entryType,
            entryTypeKey = entryTypeKey,
            options =
                styles.map { style ->
                    CardStyleOptionUiState(
                        styleKey = style.key,
                        titleRes = style.titleRes,
                        selected = style.key == selectedStyle.key,
                        selection = basePresentation.copy(variantKey = style.key),
                    )
                },
        )
    }

@Composable
private fun CardStyleChoices(
    group: CardStyleGroupUiState,
    onSelect: (EntryCardPresentation) -> Unit,
) {
    val typeKey = group.entryTypeKey
    var expanded by rememberSaveable(typeKey) { mutableStateOf(false) }
    val entryTypeLabel = stringResource(group.entryType.labelRes)

    SegmentedSettingsGroup(
        items =
            listOf(
                settingsGroupItem(
                    key = "interface.card_style.$typeKey.header",
                    title = entryTypeLabel,
                    onClick = { expanded = !expanded },
                    trailing = {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription =
                                stringResource(
                                    R.string.settings_interface_card_style_toggle,
                                    entryTypeLabel,
                                ),
                        )
                    },
                ),
                SegmentedSettingsItem(
                    key = "interface.card_style.$typeKey.options",
                    visible = expanded,
                ) { shapes ->
                    SegmentedListItem(
                        shapes = shapes,
                        colors = settingsSegmentedColors(),
                        contentPadding = PaddingValues(12.dp),
                        content = {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            ) {
                                group.options.forEach { option ->
                                    val style =
                                        CardStyleRegistry.styleFor(
                                            group.entryType,
                                            option.styleKey,
                                        )
                                    val selected = option.selected
                                    val selectStyle = { onSelect(option.selection) }
                                    Card(
                                        onClick = selectStyle,
                                        modifier =
                                            Modifier
                                                .width(280.dp)
                                                .testTag("card-style-option-$typeKey-${style.key}"),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    if (selected) {
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    },
                                            ),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = selected,
                                                    onClick = selectStyle,
                                                )
                                                Text(
                                                    text = stringResource(option.titleRes),
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                            }
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(cardStylePreviewHeight)
                                                        .testTag("card-style-preview-$typeKey-${style.key}"),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Box(Modifier.fillMaxWidth()) {
                                                    style.Preview(group.entryType, onClick = selectStyle)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                        },
                    )
                },
            ),
    )
}

private val cardStylePreviewHeight = 112.dp
