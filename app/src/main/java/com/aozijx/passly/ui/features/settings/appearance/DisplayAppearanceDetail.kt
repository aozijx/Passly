package com.aozijx.passly.ui.features.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.ui.features.settings.internal.SettingsContentActions
import com.aozijx.passly.ui.features.settings.internal.SettingsContentState
import com.aozijx.passly.ui.features.settings.shell.sectionSpacing

@Composable
internal fun DisplayAppearanceDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.Companion.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        VisualDynamicsSettingsSection(
            isDarkMode = state.isDarkMode,
            isStatusBarAutoHide = state.isStatusBarAutoHide,
            isTopBarCollapsible = state.isTopBarCollapsible,
            isTabBarCollapsible = state.isTabBarCollapsible,
            onDarkModeChange = actions.onDarkModeChange,
            onStatusBarAutoHideChange = actions.onStatusBarAutoHideChange,
            onTopBarCollapsibleChange = actions.onTopBarCollapsibleChange,
            onTabBarCollapsibleChange = actions.onTabBarCollapsibleChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppearanceCustomizationSettingsSection(
            isDynamicColor = state.isDynamicColor,
            onDynamicColorChange = actions.onDynamicColorChange,
            availableStyles = state.availableCardStyles,
            passwordSelectedStyle = state.passwordSelectedStyle,
            totpSelectedStyle = state.totpSelectedStyle,
            onPasswordStyleSelected = actions.onPasswordStyleSelected,
            onTotpStyleSelected = actions.onTotpStyleSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        LanguageSettingsSection(
            currentLanguage = "简体中文",
            onLanguageClick = {}
        )

        Spacer(modifier = Modifier.height(24.dp))

        VaultTabsSettingsSection(
            visibleVaultTabs = state.visibleVaultTabs,
            tabBarMaxTabsWithoutScroll = state.tabBarMaxTabsWithoutScroll,
            onTabBarMaxTabsWithoutScrollChange = actions.onTabBarMaxTabsWithoutScrollChange,
            onVisibleVaultTabsChange = actions.onVisibleVaultTabsChange
        )
    }
}