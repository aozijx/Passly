package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.feature.settings.components.GroupCard
import com.aozijx.passly.feature.settings.components.switchSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup
import com.aozijx.passly.feature.settings.shell.sectionSpacing

@Composable
internal fun InterfaceDetail(
    state: InterfaceUiState,
    availableCardStyles: List<VaultCardStyle>,
    loginSelectedStyle: VaultCardStyle,
    onStatusBarAutoHideChange: (Boolean) -> Unit,
    onTopBarCollapsibleChange: (Boolean) -> Unit,
    onTabBarCollapsibleChange: (Boolean) -> Unit,
    onLoginStyleSelected: (VaultCardStyle) -> Unit,
    onVisibleVaultTabsChange: (Set<String>) -> Unit,
    onTabBarMaxTabsWithoutScrollChange: (Int) -> Unit
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsGroupTitle(text = "沉浸式体验")
        SettingsRoundedGroup {
            switchSettingsItem(
                icon = Icons.Default.Fullscreen,
                title = "自动隐藏系统状态栏",
                subtitle = "浏览列表时释放屏幕顶部空间",
                checked = state.isStatusBarAutoHide,
                onCheckedChange = onStatusBarAutoHideChange
            )
            switchSettingsItem(
                icon = Icons.Default.ViewDay,
                title = "标题栏跟随滚动",
                subtitle = "上滑时自动收缩标题以获得更多视野",
                checked = state.isTopBarCollapsible,
                onCheckedChange = onTopBarCollapsibleChange
            )
            switchSettingsItem(
                icon = Icons.Default.SpaceDashboard,
                title = "分类标签栏跟随滚动",
                subtitle = "功能分类标签随列表滑动智能隐藏",
                checked = state.isTabBarCollapsible,
                onCheckedChange = onTabBarCollapsibleChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroupTitle(text = "卡片样式")
        SettingsRoundedGroup {
            item { position ->
                GroupCard(position = position, contentPadding = PaddingValues(0.dp)) {
                    CardStyleSettingsSection(
                        availableStyles = availableCardStyles,
                        loginSelectedStyle = loginSelectedStyle,
                        onLoginStyleSelected = onLoginStyleSelected
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        VaultTabsSettingsSection(
            visibleVaultTabs = state.visibleVaultTabs,
            tabBarMaxTabsWithoutScroll = state.tabBarMaxTabsWithoutScroll,
            onTabBarMaxTabsWithoutScrollChange = onTabBarMaxTabsWithoutScrollChange,
            onVisibleVaultTabsChange = onVisibleVaultTabsChange
        )
    }
}