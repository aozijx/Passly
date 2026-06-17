package com.aozijx.passly.ui.features.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

/**
 * 通用设置项，支持自定义尾部内容（Switch、图标按钮、文字等）。
 */
fun RoundedGroupScope.settingsItem(
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    item(visible = visible) { position ->
        GroupCard(
            position = position,
            contentPadding = PaddingValues(0.dp),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                } else if (iconPlaceholder) {
                    // 无图标时占位，保持与其他有图标项对齐
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                trailing()
            }
        }
    }
}

/**
 * Switch 设置项。点击整行可切换开关状态。
 */
fun RoundedGroupScope.switchSettingsItem(
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    settingsItem(
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
}

/**
 * 导航设置项，右侧显示可选的数值文字，点击跳转到子页面。
 */
fun RoundedGroupScope.navigationSettingsItem(
    visible: Boolean = true,
    icon: ImageVector? = null,
    iconPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit
) {
    settingsItem(
        visible = visible,
        icon = icon,
        iconPlaceholder = iconPlaceholder,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            if (!value.isNullOrBlank()) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { it } togetherWith slideOutVertically { -it })
                    },
                    label = "setting_value"
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    )
}