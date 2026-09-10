package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.ui.shared.components.group.model.SegmentedSettingsItem

/** 通用设置项：图标 + 标题/副标题 + 可选加载态与选中背景。 */
fun settingsGroupItem(
    key: String,
    visible: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    isLoading: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
): SegmentedSettingsItem = SegmentedSettingsItem(key = key, visible = visible) { shapes ->
    val trailingContent: (@Composable () -> Unit)? =
        if (isLoading) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            trailing
        }
    val leadingContent = icon.asLeadingContent()
    val supportingText = subtitle
    val supportingContent: (@Composable () -> Unit)? =
        if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                )
            }
        } else {
            null
        }
    val content: @Composable () -> Unit = {
        Text(
            text = title,
        )
    }
    when {
        onClick != null && selected ->
            SegmentedListItem(
                selected = true,
                onClick = onClick,
                shapes = shapes,
                colors = settingsSegmentedColors(),
                enabled = enabled,
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                content = content,
            )
        onClick != null ->
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                colors = settingsSegmentedColors(),
                enabled = enabled,
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                content = content,
            )
        else ->
            SegmentedListItem(
                shapes = shapes,
                colors = settingsSegmentedColors(),
                enabled = enabled,
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                content = content,
            )
    }
}
