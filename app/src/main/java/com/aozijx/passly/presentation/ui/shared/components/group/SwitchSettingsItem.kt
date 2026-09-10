package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.shared.components.group.model.SegmentedSettingsItem

/** 开关设置项：点击整行切换，右侧为 Switch。 */
fun switchSettingsGroupItem(
    key: String,
    visible: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    reserveLeadingIconSpace: Boolean = false,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
): SegmentedSettingsItem = SegmentedSettingsItem(key = key, visible = visible) { shapes ->
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes.withStableSelectionShape(),
        colors = settingsSegmentedColors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = icon.asLeadingContent(reserveLeadingIconSpace),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent =
                    if (checked) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        },
        supportingContent = subtitle?.let { value ->
            {
                Text(
                    text = value,
                )
            }
        },
        content = {
            Text(
                text = title,
            )
        },
    )
}

internal fun ListItemShapes.withStableSelectionShape(): ListItemShapes =
    copy(selectedShape = shape)
