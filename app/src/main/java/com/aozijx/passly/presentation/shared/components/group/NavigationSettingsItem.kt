package com.aozijx.passly.presentation.shared.components.group

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.shared.components.group.model.SegmentedSettingsItem
import com.aozijx.passly.presentation.shared.components.group.model.SettingsItemValue

/** 导航设置项：点击跳转/执行动作，右侧可显示当前值。 */
fun navigationSettingsGroupItem(
    key: String,
    visible: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    reserveLeadingIconSpace: Boolean = false,
    title: String,
    subtitle: String? = null,
    value: SettingsItemValue = SettingsItemValue.Empty,
    selected: Boolean = false,
    onClick: () -> Unit,
): SegmentedSettingsItem =
    settingsGroupItem(
        key = key,
        visible = visible,
        enabled = enabled,
        icon = icon,
        reserveLeadingIconSpace = reserveLeadingIconSpace,
        title = title,
        subtitle = subtitle,
        selected = selected,
        onClick = onClick,
        trailing =
            when (value) {
                SettingsItemValue.Empty -> {
                    null
                }

                SettingsItemValue.Unavailable -> {
                    null
                }

                SettingsItemValue.Loading -> {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is SettingsItemValue.Text -> {
                    {
                        AnimatedSettingValue(value = value.value, enabled = enabled)
                    }
                }
            },
    )
