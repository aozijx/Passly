package com.aozijx.passly.core.ui.components.group

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import com.aozijx.passly.core.ui.components.group.model.RoundedGroupItem

/** 最底层的自定义设置项：完全由调用方提供行内容。 */
fun customSettingsItem(
    key: String,
    visible: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    containerColor: (@Composable () -> Color)? = null,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
): RoundedGroupItem = RoundedGroupItem(key = key, visible = visible) { itemScope ->
    GroupCard(
        itemScope = itemScope,
        contentPadding = contentPadding ?: itemScope.contentPadding,
        containerColor = containerColor?.invoke() ?: itemScope.containerColor,
        onClick = onClick
    ) {
        SettingsItemRow(leading = leading, content = content, trailing = trailing)
    }
}

/** 通用设置项：图标 + 标题/副标题 + 可选加载态与选中背景。 */
fun settingsGroupItem(
    key: String,
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    isLoading: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
): RoundedGroupItem = customSettingsItem(
    key = key,
    visible = visible,
    onClick = onClick,
    containerColor = if (selected) {
        { MaterialTheme.colorScheme.secondaryContainer }
    } else {
        null
    },
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
