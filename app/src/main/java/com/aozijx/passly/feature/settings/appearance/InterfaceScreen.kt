package com.aozijx.passly.feature.settings.appearance

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
internal fun InterfaceDetail(
    state: InterfaceUiState,
    onStatusBarAutoHideChange: (Boolean) -> Unit,
    onTopBarCollapsibleChange: (Boolean) -> Unit,
    onTabBarCollapsibleChange: (Boolean) -> Unit,
    onVisibleVaultTabsChange: (Set<String>) -> Unit,
    onTabBarMaxTabsWithoutScrollChange: (Int) -> Unit
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_interface_immersive_title))
        RoundedGroup(
            items = listOf(
                switchSettingsGroupItem(
                    key = "interface.status_bar_auto_hide",
                    icon = Icons.Default.Fullscreen,
                    title = stringResource(R.string.settings_interface_hide_status_bar),
                    subtitle = stringResource(R.string.settings_interface_hide_status_bar_desc),
                    checked = state.hideSystemBars,
                    onCheckedChange = onStatusBarAutoHideChange
                ),
                switchSettingsGroupItem(
                    key = "interface.top_bar_collapsible",
                    icon = Icons.Default.ViewDay,
                    title = stringResource(R.string.settings_interface_top_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_top_bar_collapsible_desc),
                    checked = state.collapseTopBarOnScroll,
                    onCheckedChange = onTopBarCollapsibleChange
                ),
                switchSettingsGroupItem(
                    key = "interface.tab_bar_collapsible",
                    icon = Icons.Default.SpaceDashboard,
                    title = stringResource(R.string.settings_interface_tab_bar_collapsible),
                    subtitle = stringResource(R.string.settings_interface_tab_bar_collapsible_desc),
                    checked = state.collapseTabBarOnScroll,
                    onCheckedChange = onTabBarCollapsibleChange
                )
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
