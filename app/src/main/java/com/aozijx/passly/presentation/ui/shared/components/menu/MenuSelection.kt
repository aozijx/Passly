package com.aozijx.passly.presentation.ui.shared.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 下拉菜单选中项的高亮样式：primaryContainer 圆角背景 + 紧凑内边距。
 * 设置下拉框（SettingsGroupItems）与首页更多菜单（VaultMenuPages）共用，
 * 保证全应用菜单选中态一致。
 */
@Composable
fun Modifier.selectedMenuModifier(selected: Boolean): Modifier =
    this
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(MaterialTheme.shapes.small)
        .background(
            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )

/**
 * 下拉菜单选项文本：选中时用主题色加粗，未选中用普通色。
 * 将"选中态决定颜色/字重"的条件收敛到此组件，避免各调用处重复 if/else。
 */
@Composable
fun MenuOptionText(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = style,
        color = if (selected) selectedColor else unselectedColor,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
    )
}
