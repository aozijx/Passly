package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.shared.components.group.model.SegmentedSettingsItem

/** 滑条设置项：标题行 + 下方 Slider。 */
fun sliderSettingsGroupItem(
    key: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
): SegmentedSettingsItem = SegmentedSettingsItem(key = key) { shapes ->
    SegmentedListItem(
        shapes = shapes,
        colors = settingsSegmentedColors(),
        enabled = enabled,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon.asLeadingContent()?.let { leadingContent ->
                        leadingContent()
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                subtitle?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(
                            start = if (icon != null) 40.dp else 0.dp
                        ),
                    )
                }
                Slider(
                    value = value.coerceIn(valueRange),
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
