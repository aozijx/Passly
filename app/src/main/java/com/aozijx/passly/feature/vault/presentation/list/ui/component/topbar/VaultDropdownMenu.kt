package com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.presentation.list.VaultRouteRequest
import com.aozijx.passly.feature.vault.presentation.list.VaultUiAction

private enum class VaultMenuPage { MAIN, SORT, CATEGORY_FILTER }

@Composable
internal fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: VaultMenuUiState,
    onAction: (VaultUiAction) -> Unit,
    onRouteRequest: (VaultRouteRequest) -> Unit,
) {
    var currentPage by remember(expanded) { mutableStateOf(VaultMenuPage.MAIN) }
    var categorySearchVisible by remember(expanded) { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(categorySearchVisible) {
        if (categorySearchVisible) categoryFocusRequester.requestFocus()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            onAction(VaultUiAction.CategorySearchQueryChanged(""))
            onDismissRequest()
        },
    ) {
        when (currentPage) {
            VaultMenuPage.MAIN -> {
                MainMenuContent(
                    onSortClick = { currentPage = VaultMenuPage.SORT },
                    onCategoryFilterClick = { currentPage = VaultMenuPage.CATEGORY_FILTER },
                    showTotpCode = uiState.showTotpCode,
                    onToggleTotpVisibility = {
                        onAction(VaultUiAction.ToggleShowTotpCode)
                    },
                    onDismissRequest = onDismissRequest,
                    onRouteRequest = onRouteRequest,
                )
            }

            VaultMenuPage.SORT -> {
                SortSubMenu(
                    selectedSort = uiState.selectedSort,
                    sortOptions = uiState.sortOptions,
                    onSortSelected = {
                        onAction(VaultUiAction.SortOptionSelected(it))
                    },
                    onBack = { currentPage = VaultMenuPage.MAIN },
                )
            }

            VaultMenuPage.CATEGORY_FILTER -> {
                FilterSubMenu(
                    searchLabelRes = R.string.vault_search_category,
                    searchHintRes = R.string.vault_search_category_hint,
                    isSearchVisible = categorySearchVisible,
                    onToggleSearch = {
                        categorySearchVisible = it
                        if (!it) onAction(VaultUiAction.CategorySearchQueryChanged(""))
                    },
                    searchQuery = uiState.categorySearchQuery,
                    onSearchQueryChange = { onAction(VaultUiAction.CategorySearchQueryChanged(it)) },
                    focusRequester = categoryFocusRequester,
                    items = uiState.matchingCategories,
                    categoriesState = uiState.categoriesState,
                    onRetry = { onAction(VaultUiAction.RetryCategories) },
                    selectedItem = uiState.categorySelection,
                    itemText = { it },
                    onItemSelected = {
                        onAction(VaultUiAction.CategorySelectionChanged(it))
                        onDismissRequest()
                    },
                    onBack = {
                        if (categorySearchVisible) {
                            categorySearchVisible = false
                            onAction(VaultUiAction.CategorySearchQueryChanged(""))
                        } else {
                            currentPage = VaultMenuPage.MAIN
                        }
                    },
                )
            }
        }
    }
}
