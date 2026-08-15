package com.aozijx.passly.core.ui.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
