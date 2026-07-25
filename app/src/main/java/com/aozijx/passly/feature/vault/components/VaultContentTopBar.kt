package com.aozijx.passly.feature.vault.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.feature.vault.components.topbar.VaultTopBar
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.VaultTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContentTopBar(
    uiState: VaultUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingsClick: () -> Unit,
    isStatusBarAutoHide: Boolean,
    isTopBarCollapsible: Boolean,
    isTabBarCollapsible: Boolean,
    isDatabaseInitializing: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onClearCategory: () -> Unit,
    onExpandMoreMenu: (Boolean) -> Unit,
    onToggleTotpVisibility: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (VaultSortSpec) -> Unit,
    onSelectTab: (VaultTab) -> Unit
) {
    Column {
        VaultTopBar(
            uiState = uiState,
            scrollBehavior = scrollBehavior,
            onSettingsClick = onSettingsClick,
            isStatusBarAutoHide = isStatusBarAutoHide,
            isTopBarCollapsible = isTopBarCollapsible,
            isTabBarCollapsible = isTabBarCollapsible,
            onSearchQueryChange = onSearchQueryChange,
            onToggleSearch = onToggleSearch,
            onClearCategory = onClearCategory,
            onExpandMoreMenu = onExpandMoreMenu,
            onToggleTotpVisibility = onToggleTotpVisibility,
            onCategorySelected = onCategorySelected,
            onSortSelected = onSortSelected,
            onSelectTab = onSelectTab
        )

        if (uiState.isVaultItemsLoading || isDatabaseInitializing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
