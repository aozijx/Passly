package com.aozijx.passly.core.ui.components.group

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem

/** 滑条设置项：标题行 + 下方 Slider。 */
fun sliderSettingsGroupItem(
    key: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
): RoundedGroupItem = RoundedGroupItem(key = key) { itemScope ->
    GroupCard(itemScope = itemScope) {
        SettingsItemRow(
            leading = icon.asLeadingContent(iconPlaceholder),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}
