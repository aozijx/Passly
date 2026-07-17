package com.aozijx.passly.feature.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.feature.vault.components.cardstyle.CardStyleRegistry

private val SETTINGS_GROUP_TITLE_BY_TYPE: Map<EntryType, Int> = mapOf(
    EntryType.LOGIN to R.string.settings_card_style_group_password
)

private data class SettingsGroupSpec(
    @field:androidx.annotation.StringRes val titleRes: Int,
    val entryType: EntryType,
    val styleCandidates: List<VaultCardStyle>
) {
    val entryTypeName: String get() = entryType.name
}

private data class TypeStylePolicy(
    val defaultStyle: VaultCardStyle,
    val selectableStyles: List<VaultCardStyle>
)

private val TYPE_STYLE_POLICY_MAP: Map<EntryType, TypeStylePolicy> =
    EntryType.entries.associateWith {
        TypeStylePolicy(
            defaultStyle = VaultCardStyle.DEFAULT,
            selectableStyles = listOf(VaultCardStyle.DEFAULT, VaultCardStyle.PASSWORD)
        )
    }

private val SETTINGS_GROUP_SPECS: List<SettingsGroupSpec> =
    SETTINGS_GROUP_TITLE_BY_TYPE.map { (entryType, titleRes) ->
        SettingsGroupSpec(
            titleRes = titleRes,
            entryType = entryType,
            styleCandidates = TYPE_STYLE_POLICY_MAP.getValue(entryType).selectableStyles
        )
    }

@Composable
fun CardStyleSettingsSection(
    availableStyles: List<VaultCardStyle>,
    loginSelectedStyle: VaultCardStyle,
    onLoginStyleSelected: (VaultCardStyle) -> Unit
) {
    val expandedState = rememberSaveable { mutableStateOf(false) }
    var expandedGroupTypes by rememberSaveable { mutableStateOf(setOf<String>()) }
    val selectedStyleByType = mapOf(
        EntryType.LOGIN.name to loginSelectedStyle
    )
    val onStyleSelectedByType = mapOf(
        EntryType.LOGIN.name to onLoginStyleSelected
    )
    val groups = SETTINGS_GROUP_SPECS.map { spec ->
        spec to spec.styleCandidates.filter { it in availableStyles }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedState.value = !expandedState.value }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ViewDay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_card_style_section_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (expandedState.value) {
                        stringResource(R.string.settings_card_style_section_expanded_hint)
                    } else {
                        stringResource(R.string.settings_card_style_section_collapsed_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expandedState.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }

        AnimatedVisibility(
            visible = expandedState.value,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { (spec, styles) ->
                    val onStyleSelected =
                        onStyleSelectedByType[spec.entryTypeName] ?: return@forEach
                    val selectedStyle =
                        selectedStyleByType[spec.entryTypeName] ?: VaultCardStyle.DEFAULT
                    val expanded = spec.entryTypeName in expandedGroupTypes
                    CardStyleGroup(
                        spec = spec,
                        styles = styles,
                        selectedStyle = selectedStyle,
                        expanded = expanded,
                        onToggle = {
                            expandedGroupTypes = if (expanded) {
                                expandedGroupTypes - spec.entryTypeName
                            } else {
                                expandedGroupTypes + spec.entryTypeName
                            }
                        },
                        onStyleSelected = onStyleSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun CardStyleGroup(
    spec: SettingsGroupSpec,
    styles: List<VaultCardStyle>,
    selectedStyle: VaultCardStyle,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStyleSelected: (VaultCardStyle) -> Unit
) {
    StyleGroup(
        title = stringResource(spec.titleRes), expanded = expanded, onToggle = onToggle
    ) {
        styles.forEach { style ->
                    CardStyleOption(
                        style = style,
                        selected = style == selectedStyle,
                        entryTypeValue = spec.entryTypeName,
                        onClick = { onStyleSelected(style) })
                }
    }
}

@Composable
private fun StyleGroup(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupHeaderButton(
            title = title, expanded = expanded, onClick = onToggle
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun GroupHeaderButton(
    title: String, expanded: Boolean, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp, if (expanded) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (expanded) {
                    stringResource(R.string.settings_card_style_group_expanded_label)
                } else {
                    stringResource(R.string.settings_card_style_group_collapsed_label)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CardStyleOption(
    style: VaultCardStyle, selected: Boolean, entryTypeValue: String, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onClick)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(style.displayNameRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(style.descriptionRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CardStyleRegistry.RenderPreviewVaultItem(
                style = style, onClick = onClick
            )
        }
    }
}
