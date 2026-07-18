package com.aozijx.passly.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SettingsSectionStyle(
    val verticalPadding: Dp = 8.dp,
    val titleStartPadding: Dp = 8.dp,
    val titleBottomPadding: Dp = 8.dp
)

object SettingsSectionDefaults {
    val style = SettingsSectionStyle()
}

/**
 * 设置页的通用区块容器。布局参数和内容都由外部提供，不持有界面状态。
 */
@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    style: SettingsSectionStyle = SettingsSectionDefaults.style,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = style.verticalPadding),
        content = content
    )
}

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: SettingsSectionStyle = SettingsSectionDefaults.style
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(
            start = style.titleStartPadding,
            bottom = style.titleBottomPadding
        )
    )
}
