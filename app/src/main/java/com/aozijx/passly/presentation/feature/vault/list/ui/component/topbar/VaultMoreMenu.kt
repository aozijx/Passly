package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.toFeatureModel

@Composable
internal fun VaultMoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: VaultTopBarUiState,
    onAction: (VaultUiAction) -> Unit,
    onSettingsClick: () -> Unit,
) {
    VaultDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        showTOTPCode = uiState.showTotpCode,
        onToggleTotpVisibility = {
            onAction(VaultUiAction.ToggleShowTotpCode)
        },
        onSettingsClick = onSettingsClick,
        availableCategories = uiState.availableCategories,
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = {
            onAction(VaultUiAction.CategorySelected(it))
        },
        selectedSort = uiState.selectedSort,
        onSortSelected = {
            onAction(VaultUiAction.SortOptionSelected(it.toFeatureModel()))
        },
    )
}
