package com.aozijx.passly.feature.settings.appearance

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
import com.aozijx.passly.core.ui.components.group.GroupCard
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.RoundedGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.VaultCardStyle

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
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionTitle(text = "沉浸式体验")
        RoundedGroup(
            items = listOf(
                switchSettingsGroupItem(
                    key = "interface.status_bar_auto_hide",
                    icon = Icons.Default.Fullscreen,
                    title = "自动隐藏系统状态栏",
                    subtitle = "浏览列表时释放屏幕顶部空间",
                    checked = state.isStatusBarAutoHide,
                    onCheckedChange = onStatusBarAutoHideChange
                ),
                switchSettingsGroupItem(
                    key = "interface.top_bar_collapsible",
                    icon = Icons.Default.ViewDay,
                    title = "标题栏跟随滚动",
                    subtitle = "上滑时自动收缩标题以获得更多视野",
                    checked = state.isTopBarCollapsible,
                    onCheckedChange = onTopBarCollapsibleChange
                ),
                switchSettingsGroupItem(
                    key = "interface.tab_bar_collapsible",
                    icon = Icons.Default.SpaceDashboard,
                    title = "分类标签栏跟随滚动",
                    subtitle = "功能分类标签随列表滑动智能隐藏",
                    checked = state.isTabBarCollapsible,
                    onCheckedChange = onTabBarCollapsibleChange
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle(text = "卡片样式")
        RoundedGroup(
            items = listOf(
                RoundedGroupItem(key = "interface.card_style") { itemScope ->
                    GroupCard(itemScope = itemScope, contentPadding = PaddingValues(0.dp)) {
                        CardStyleSettingsSection(
                            availableStyles = availableCardStyles,
                            loginSelectedStyle = loginSelectedStyle,
                            onLoginStyleSelected = onLoginStyleSelected
                        )
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        VaultTabsSettingsSection(
            visibleVaultTabs = state.visibleVaultTabs,
            tabBarMaxTabsWithoutScroll = state.tabBarMaxTabsWithoutScroll,
            onTabBarMaxTabsWithoutScrollChange = onTabBarMaxTabsWithoutScrollChange,
            onVisibleVaultTabsChange = onVisibleVaultTabsChange
        )
    }
}
