package com.aozijx.passly.features.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.internal.SettingsContentActions
import com.aozijx.passly.features.settings.internal.SettingsContentState
import com.aozijx.passly.features.settings.shell.sectionSpacing

@Composable
internal fun DisplayAppearanceDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        ImmersiveExperienceSettingsSection(
            isStatusBarAutoHide = state.isStatusBarAutoHide,
            isTopBarCollapsible = state.isTopBarCollapsible,
            isTabBarCollapsible = state.isTabBarCollapsible,
            onStatusBarAutoHideChange = actions.onStatusBarAutoHideChange,
            onTopBarCollapsibleChange = actions.onTopBarCollapsibleChange,
            onTabBarCollapsibleChange = actions.onTabBarCollapsibleChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppearanceCustomizationSettingsSection(
            availableStyles = state.availableCardStyles,
            passwordSelectedStyle = state.passwordSelectedStyle,
            totpSelectedStyle = state.totpSelectedStyle,
            onPasswordStyleSelected = actions.onPasswordStyleSelected,
            onTotpStyleSelected = actions.onTotpStyleSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        DarkModeSettingsSection(
            isDarkMode = false,
            onDarkModeChange = {}
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