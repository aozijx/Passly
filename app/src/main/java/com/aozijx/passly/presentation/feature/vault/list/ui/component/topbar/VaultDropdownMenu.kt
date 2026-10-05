package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.toFeatureModel

@Composable
internal fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: VaultMenuUiState,
    onAction: (VaultUiAction) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var currentPage by remember(expanded) { mutableStateOf(VaultMenuPage.MAIN) }
    var categorySearchQuery by remember(expanded) { mutableStateOf("") }
    var categorySearchVisible by remember(expanded) { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }
    val motionScheme = MaterialTheme.motionScheme

    LaunchedEffect(categorySearchVisible) {
        if (categorySearchVisible) categoryFocusRequester.requestFocus()
    }

    val filteredCategories = remember(uiState.availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) uiState.availableCategories
        else uiState.availableCategories.filter {
            it.contains(categorySearchQuery, ignoreCase = true)
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.animateContentSize(
            animationSpec = motionScheme.defaultSpatialSpec(),
        ),
    ) {
        VaultMenuPageTransition(
            currentPage = currentPage,
            mainContent = {
                Column {
                    MainMenuContent(
                        onSortClick = { currentPage = VaultMenuPage.SORT },
                        onCategoryFilterClick = {
                            currentPage = VaultMenuPage.CATEGORY_FILTER
                        },
                        showTOTPCode = uiState.showTotpCode,
                        onToggleTotpVisibility = {
                            onAction(VaultUiAction.ToggleShowTotpCode)
                        },
                        onDismissRequest = onDismissRequest,
                        onSettingsClick = onSettingsClick,
                    )
                }
            },
            sortContent = {
                Column {
                    SortSubMenu(
                        selectedSort = uiState.selectedSort,
                        onSortSelected = {
                            onAction(VaultUiAction.SortOptionSelected(it.toFeatureModel()))
                        },
                        onBack = { currentPage = VaultMenuPage.MAIN },
                    )
                }
            },
            categoryFilterContent = {
                Column {
                    FilterSubMenu(
                        searchLabelRes = R.string.vault_search_category,
                        searchHintRes = R.string.vault_search_category_hint,
                        isSearchVisible = categorySearchVisible,
                        onToggleSearch = { categorySearchVisible = it },
                        searchQuery = categorySearchQuery,
                        onSearchQueryChange = { categorySearchQuery = it },
                        focusRequester = categoryFocusRequester,
                        items = filteredCategories,
                        selectedItem = uiState.selectedCategory,
                        itemText = { it },
                        onItemSelected = {
                            onAction(VaultUiAction.CategorySelected(it))
                            onDismissRequest()
                        },
                        onBack = {
                            if (categorySearchVisible) {
                                categorySearchVisible = false
                                categorySearchQuery = ""
                            } else {
                                currentPage = VaultMenuPage.MAIN
                            }
                        },
                    )
                }
            },
        )
    }
}
