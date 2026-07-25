package com.aozijx.passly.core.ui.components.group

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun customSettingsItem(
    key: String,
    visible: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
): RoundedGroupItem = RoundedGroupItem(key = key, visible = visible) { itemScope ->
    GroupCard(
        itemScope = itemScope,
        contentPadding = contentPadding ?: itemScope.contentPadding,
        onClick = onClick
    ) {
        SettingsItemRow(leading = leading, content = content, trailing = trailing)
    }
}

fun settingsGroupItem(
    key: String,
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
): RoundedGroupItem = customSettingsItem(
    key = key,
    visible = visible,
    onClick = onClick,
    leading = icon.asLeadingContent(iconPlaceholder),
    content = {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    },
    trailing = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            trailing()
        }
    }
)

fun switchSettingsGroupItem(
    key: String,
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
): RoundedGroupItem = settingsGroupItem(
    key = key,
    visible = visible,
    icon = icon,
    iconPlaceholder = iconPlaceholder,
    title = title,
    subtitle = subtitle,
    onClick = { onCheckedChange(!checked) },
    trailing = {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
)

fun navigationSettingsGroupItem(
    key: String,
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    isLoading: Boolean = false,
    onClick: () -> Unit
): RoundedGroupItem = settingsGroupItem(
    key = key,
    visible = visible,
    icon = icon,
    iconPlaceholder = iconPlaceholder,
    title = title,
    subtitle = subtitle,
    isLoading = isLoading,
    onClick = onClick,
    trailing = {
        value?.takeIf(String::isNotBlank)?.let {
            AnimatedSettingValue(value = it)
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
fun <T> dropdownSettingsGroupItem(
    key: String,
    icon: ImageVector? = null,
    title: String,
    selected: T,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit
): RoundedGroupItem = RoundedGroupItem(key = key) { itemScope ->
    GroupCard(
        itemScope = itemScope,
        onClick = { onExpandedChange(!expanded) }
    ) {
        SettingsItemRow(
            leading = icon.asLeadingContent(false),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            trailing = {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = onExpandedChange
                ) {
                    AnimatedSettingValue(
                        value = selectedLabel,
                        modifier = Modifier
                            .menuAnchor(
                                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                true
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) },
                        matchAnchorWidth = false
                    ) {
                        options.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (value == selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (value == selected) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    onSelect(value)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AnimatedSettingValue(
    value: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            slideInVertically { it } togetherWith slideOutVertically { -it }
        },
        label = "setting_value"
    ) { targetValue ->
        Text(
            text = targetValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SettingsItemRow(
    leading: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), content = content)
        trailing()
    }
}

private fun ImageVector?.asLeadingContent(
    placeholder: Boolean
): (@Composable () -> Unit)? = when {
    this != null -> {
        val image = this
        {
            Icon(
                imageVector = image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }

    placeholder -> {
        { Spacer(modifier = Modifier.width(40.dp)) }
    }

    else -> null
}
