package com.aozijx.passly.core.ui.components.group

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem

/** 开关设置项：点击整行切换，右侧为 Switch。 */
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
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
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
)
