package com.aozijx.passly.features.settings.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.components.switchSettingsItem
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SettingsRoundedGroup

@Composable
fun VisualDynamicsSettingsSection(
    isDarkMode: Boolean,
    isStatusBarAutoHide: Boolean,
    isTopBarCollapsible: Boolean,
    isTabBarCollapsible: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStatusBarAutoHideChange: (Boolean) -> Unit,
    onTopBarCollapsibleChange: (Boolean) -> Unit,
    onTabBarCollapsibleChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "视觉动态")
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
            title = "深色模式",
            subtitle = if (isDarkMode) "当前为深色主题" else "当前为浅色主题",
            checked = isDarkMode,
            onCheckedChange = onDarkModeChange
        )
        switchSettingsItem(
            icon = Icons.Default.Fullscreen,
            title = "自动隐藏系统状态栏",
            subtitle = "浏览列表时释放屏幕顶部空间",
            checked = isStatusBarAutoHide,
            onCheckedChange = onStatusBarAutoHideChange
        )
        switchSettingsItem(
            icon = Icons.Default.ViewDay,
            title = "标题栏跟随滚动",
            subtitle = "上滑时自动收缩标题以获得更多视野",
            checked = isTopBarCollapsible,
            onCheckedChange = onTopBarCollapsibleChange
        )
        switchSettingsItem(
            icon = Icons.Default.SpaceDashboard,
            title = "分类标签栏跟随滚动",
            subtitle = "功能分类标签随列表滑动智能隐藏",
            checked = isTabBarCollapsible,
            onCheckedChange = onTabBarCollapsibleChange
        )
    }
}