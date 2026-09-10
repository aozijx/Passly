package com.aozijx.passly.presentation.ui.shared.components.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.presentation.ui.shared.components.group.model.SegmentedSettingsItem

/** 导航设置项：点击跳转/执行动作，右侧可显示当前值。 */
fun navigationSettingsGroupItem(
    key: String,
    visible: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    isLoading: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
): SegmentedSettingsItem = settingsGroupItem(
    key = key,
    visible = visible,
    enabled = enabled,
    icon = icon,
    title = title,
    subtitle = subtitle,
    isLoading = isLoading,
    selected = selected,
    onClick = onClick,
    trailing = {
        value?.takeIf(String::isNotBlank)?.let {
            AnimatedSettingValue(value = it, enabled = enabled)
        }
    }
)
