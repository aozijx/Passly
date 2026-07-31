package com.aozijx.passly.feature.vault.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.settings.model.VaultSortSpec

private enum class MenuPage { MAIN, SORT, CATEGORY_FILTER, ENTRY_TYPE_FILTER }

@Composable
fun VaultDropdownMenu(
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onSettingsClick: () -> Unit,
    availableEntryTypes: List<String>,
    availableCategories: List<String>,
    selectedEntryTypeName: String?,
    selectedCategory: String?,
    onEntryTypeSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    selectedSort: VaultSortSpec,
    onSortSelected: (VaultSortSpec) -> Unit
) {
    var currentPage by remember { mutableStateOf(MenuPage.MAIN) }
    var entryTypeSearchQuery by remember { mutableStateOf("") }
    var entryTypeSearchVisible by remember { mutableStateOf(false) }
    val entryTypeFocusRequester = remember { FocusRequester() }
    var categorySearchQuery by remember { mutableStateOf("") }
    var categorySearchVisible by remember { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(entryTypeSearchVisible) {
        if (entryTypeSearchVisible) entryTypeFocusRequester.requestFocus()
    }
    LaunchedEffect(categorySearchVisible) {
        if (categorySearchVisible) categoryFocusRequester.requestFocus()
    }

    val filteredEntryTypes = remember(availableEntryTypes, entryTypeSearchQuery) {
        if (entryTypeSearchQuery.isBlank()) availableEntryTypes
        else availableEntryTypes.filter {
            it.contains(entryTypeSearchQuery, ignoreCase = true) ||
                EntryType.fromName(it).displayName.contains(
                    entryTypeSearchQuery,
                    ignoreCase = true
                )
        }
    }
    val filteredCategories = remember(availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter {
            it.contains(categorySearchQuery, ignoreCase = true)
        }
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 150.dp)
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val transition = if (targetState == MenuPage.MAIN) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                }
                transition.using(SizeTransform(clip = false))
            },
            label = "MenuPageTransition"
        ) { page ->
            Column(Modifier.fillMaxWidth()) {
                when (page) {
                    MenuPage.MAIN -> MainMenuContent(
                        onSortClick = { currentPage = MenuPage.SORT },
                        onCategoryFilterClick = { currentPage = MenuPage.CATEGORY_FILTER },
                        onEntryTypeFilterClick = { currentPage = MenuPage.ENTRY_TYPE_FILTER },
                        showTOTPCode = showTOTPCode,
                        onToggleTotpVisibility = onToggleTotpVisibility,
                        onDismissRequest = onDismissRequest,
                        onSettingsClick = onSettingsClick
                    )
                    MenuPage.SORT -> SortSubMenu(
                        selectedSort = selectedSort,
                        onSortSelected = onSortSelected,
                        onBack = { currentPage = MenuPage.MAIN }
                    )
                    MenuPage.CATEGORY_FILTER -> FilterSubMenu(
                        searchLabelRes = R.string.vault_search_category,
                        searchHintRes = R.string.vault_search_category_hint,
                        isSearchVisible = categorySearchVisible,
                        onToggleSearch = { categorySearchVisible = it },
                        searchQuery = categorySearchQuery,
                        onSearchQueryChange = { categorySearchQuery = it },
                        focusRequester = categoryFocusRequester,
                        items = filteredCategories,
                        selectedItem = selectedCategory,
                        itemText = { it },
                        onItemSelected = {
                            onCategorySelected(it)
                            onDismissRequest()
                        },
                        onBack = {
                            if (categorySearchVisible) {
                                categorySearchVisible = false
                                categorySearchQuery = ""
                            } else {
                                currentPage = MenuPage.MAIN
                            }
                        }
                    )
                    MenuPage.ENTRY_TYPE_FILTER -> FilterSubMenu(
                        searchLabelRes = R.string.vault_search_entry_type,
                        searchHintRes = R.string.vault_search_entry_type_hint,
                        isSearchVisible = entryTypeSearchVisible,
                        onToggleSearch = { entryTypeSearchVisible = it },
                        searchQuery = entryTypeSearchQuery,
                        onSearchQueryChange = { entryTypeSearchQuery = it },
                        focusRequester = entryTypeFocusRequester,
                        items = filteredEntryTypes,
                        selectedItem = selectedEntryTypeName,
                        itemText = { EntryType.fromName(it).displayName },
                        onItemSelected = {
                            onEntryTypeSelected(it)
                            onDismissRequest()
                        },
                        onBack = {
                            if (entryTypeSearchVisible) {
                                entryTypeSearchVisible = false
                                entryTypeSearchQuery = ""
                            } else {
                                currentPage = MenuPage.MAIN
                            }
                        }
                    )
                }
            }
        }
    }
}
