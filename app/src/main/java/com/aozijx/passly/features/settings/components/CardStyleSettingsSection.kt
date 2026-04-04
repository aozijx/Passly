package com.aozijx.passly.features.settings.components

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.features.settings.internal.CardStyleGroupDefinition
import com.aozijx.passly.features.settings.internal.CardStyleSettingsConfig
import com.aozijx.passly.features.vault.components.entries.VaultCardStyleRegistry

private data class StyleGroupItem(
    val definition: CardStyleGroupDefinition,
    val styles: List<VaultCardStyle>,
    val selectedStyle: VaultCardStyle,
    val expanded: Boolean,
    val onToggle: () -> Unit,
    val onStyleSelected: (VaultCardStyle) -> Unit
)

@Composable
fun CardStyleSettingsSection(
    availableStyles: List<VaultCardStyle>,
    passwordSelectedStyle: VaultCardStyle,
    totpSelectedStyle: VaultCardStyle,
    onPasswordStyleSelected: (VaultCardStyle) -> Unit,
    onTotpStyleSelected: (VaultCardStyle) -> Unit
) {
    val expandedState = rememberSaveable { mutableStateOf(false) }
    val passwordExpandedState = rememberSaveable { mutableStateOf(false) }
    val totpExpandedState = rememberSaveable { mutableStateOf(false) }
    val passwordGroupDefinition = CardStyleSettingsConfig.PASSWORD_GROUP
    val totpGroupDefinition = CardStyleSettingsConfig.TOTP_GROUP
    val groups = listOf(
        StyleGroupItem(
            definition = passwordGroupDefinition,
            styles = passwordGroupDefinition.styleCandidates.filter { it in availableStyles },
            selectedStyle = passwordSelectedStyle,
            expanded = passwordExpandedState.value,
            onToggle = { passwordExpandedState.value = !passwordExpandedState.value },
            onStyleSelected = onPasswordStyleSelected
        ),
        StyleGroupItem(
            definition = totpGroupDefinition,
            styles = totpGroupDefinition.styleCandidates.filter { it in availableStyles },
            selectedStyle = totpSelectedStyle,
            expanded = totpExpandedState.value,
            onToggle = { totpExpandedState.value = !totpExpandedState.value },
            onStyleSelected = onTotpStyleSelected
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedState.value = !expandedState.value }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ViewDay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CardStyleSettingsConfig.SECTION_TITLE,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (expandedState.value) {
                        CardStyleSettingsConfig.SECTION_EXPANDED_HINT
                    } else {
                        CardStyleSettingsConfig.SECTION_COLLAPSED_HINT
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
                groups.forEach { group ->
                    StyleGroup(
                        title = group.definition.title,
                        expanded = group.expanded,
                        onToggle = group.onToggle
                    ) {
                        group.styles.forEach { style ->
                            CardStyleOption(
                                style = style,
                                selected = style == group.selectedStyle,
                                onClick = { group.onStyleSelected(style) }
                            )
                        }
                    }
                }
            }
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
            title = title,
            expanded = expanded,
            onClick = onToggle
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
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (expanded) {
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
                    CardStyleSettingsConfig.GROUP_EXPANDED_LABEL
                } else {
                    CardStyleSettingsConfig.GROUP_COLLAPSED_LABEL
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
    style: VaultCardStyle,
    selected: Boolean,
    onClick: () -> Unit
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
                        text = style.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCardStylePreview(style = style, onClick = onClick)
        }
    }
}

@Composable
private fun SettingsCardStylePreview(style: VaultCardStyle, onClick: () -> Unit) {
    VaultCardStyleRegistry.RenderPreviewVaultItem(style = style, onClick = onClick)
}
