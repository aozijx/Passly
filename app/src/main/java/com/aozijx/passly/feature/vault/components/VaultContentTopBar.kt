package com.aozijx.passly.feature.vault.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.components.topbar.VaultTopBar
import com.aozijx.passly.feature.vault.contract.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContentTopBar(
    vaultViewModel: VaultViewModel,
    uiState: VaultUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onExportClick: () -> Unit,
    onPlainJsonExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isStatusBarAutoHide: Boolean,
    isTopBarCollapsible: Boolean,
    isTabBarCollapsible: Boolean,
    isDatabaseInitializing: Boolean = false
) {
    Column {
        VaultTopBar(
            vaultViewModel = vaultViewModel,
            uiState = uiState,
            scrollBehavior = scrollBehavior,
            onExportClick = onExportClick,
            onPlainJsonExportClick = onPlainJsonExportClick,
            onImportClick = onImportClick,
            onSettingsClick = onSettingsClick,
            isStatusBarAutoHide = isStatusBarAutoHide,
            isTopBarCollapsible = isTopBarCollapsible,
            isTabBarCollapsible = isTabBarCollapsible
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