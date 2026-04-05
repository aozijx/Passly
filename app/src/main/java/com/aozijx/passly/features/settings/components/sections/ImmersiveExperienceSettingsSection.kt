package com.aozijx.passly.features.settings.components.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle
import com.aozijx.passly.features.settings.components.common.SwitchSettingItem

@Composable
fun ImmersiveExperienceSettingsSection(
    isStatusBarAutoHide: Boolean,
    isTopBarCollapsible: Boolean,
    isTabBarCollapsible: Boolean,
    onStatusBarAutoHideChange: (Boolean) -> Unit,
    onTopBarCollapsibleChange: (Boolean) -> Unit,
    onTabBarCollapsibleChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = "沉浸式体验")
    SettingsCard {
        SwitchSettingItem(
            icon = Icons.Default.Fullscreen,
            title = "自动隐藏系统状态栏",
            subtitle = "浏览列表时释放屏幕顶部空间",
            checked = isStatusBarAutoHide,
            onCheckedChange = onStatusBarAutoHideChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.ViewDay,
            title = "标题栏跟随滚动",
            subtitle = "上滑时自动收缩标题以获得更多视野",
            checked = isTopBarCollapsible,
            onCheckedChange = onTopBarCollapsibleChange
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        SwitchSettingItem(
            icon = Icons.Default.SpaceDashboard,
            title = "分类标签栏跟随滚动",
            subtitle = "功能分类标签随列表滑动智能隐藏",
            checked = isTabBarCollapsible,
            onCheckedChange = onTabBarCollapsibleChange
        )
    }
}

