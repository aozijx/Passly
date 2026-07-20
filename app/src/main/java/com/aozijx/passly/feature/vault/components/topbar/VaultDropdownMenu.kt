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
import com.aozijx.passly.domain.model.settings.SortOption

private enum class MenuPage { MAIN, SORT, FILTER }

@Composable
fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenPlainExport: () -> Unit,
    onImportClick: () -> Unit,
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    var currentPage by remember { mutableStateOf(MenuPage.MAIN) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var categorySearchVisible by remember { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (!expanded) {
            currentPage = MenuPage.MAIN
            categorySearchQuery = ""
            categorySearchVisible = false
        }
    }
    LaunchedEffect(categorySearchVisible) {
        if (categorySearchVisible) categoryFocusRequester.requestFocus()
    }

    val filteredCategories = remember(availableCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) availableCategories
        else availableCategories.filter {
            it.contains(categorySearchQuery, ignoreCase = true)
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(min = 150.dp).animateContentSize()
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
                        onFilterClick = { currentPage = MenuPage.FILTER },
                        showTOTPCode = showTOTPCode,
                        onToggleTotpVisibility = onToggleTotpVisibility,
                        onDismissRequest = onDismissRequest,
                        onSettingsClick = onSettingsClick,
                        onExportClick = onExportClick,
                        onOpenPlainExport = onOpenPlainExport,
                        onImportClick = onImportClick
                    )
                    MenuPage.SORT -> SortSubMenu(
                        selectedSort = selectedSort,
                        onSortSelected = {
                            onSortSelected(it)
                            onDismissRequest()
                        },
                        onBack = { currentPage = MenuPage.MAIN }
                    )
                    MenuPage.FILTER -> FilterSubMenu(
                        isCategorySearchVisible = categorySearchVisible,
                        onToggleSearch = { categorySearchVisible = it },
                        categorySearchQuery = categorySearchQuery,
                        onCategorySearchQueryChange = { categorySearchQuery = it },
                        categoryFocusRequester = categoryFocusRequester,
                        filteredCategories = filteredCategories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
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
                }
            }
        }
    }
}
