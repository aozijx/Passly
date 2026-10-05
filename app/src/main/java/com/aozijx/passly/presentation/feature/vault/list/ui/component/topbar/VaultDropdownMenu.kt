package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel

private enum class MenuPage { MAIN, SORT, CATEGORY_FILTER }

@Composable
internal fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    showTOTPCode: Boolean,
    onToggleTotpVisibility: () -> Unit,
    onSettingsClick: () -> Unit,
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    selectedSort: VaultSortUiModel,
    onSortSelected: (VaultSortUiModel) -> Unit
) {
    var currentPage by remember(expanded) { mutableStateOf(MenuPage.MAIN) }
    var categorySearchQuery by remember(expanded) { mutableStateOf("") }
    var categorySearchVisible by remember(expanded) { mutableStateOf(false) }
    val categoryFocusRequester = remember { FocusRequester() }
    val motionScheme = MaterialTheme.motionScheme

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
        modifier = Modifier.widthIn(min = 150.dp)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val navigatingBack = targetState == MenuPage.MAIN
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        initialOffsetX = { width -> if (navigatingBack) -width / 4 else width },
                    ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                    initialContentExit = slideOutHorizontally(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        targetOffsetX = { width -> if (navigatingBack) width else -width / 4 },
                    ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
                    sizeTransform = null,
                )
            },
            label = "VaultMenuPage",
        ) { page ->
            Column(Modifier.fillMaxWidth()) {
                when (page) {
                    MenuPage.MAIN -> MainMenuContent(
                        onSortClick = { currentPage = MenuPage.SORT },
                        onCategoryFilterClick = { currentPage = MenuPage.CATEGORY_FILTER },
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
                }
            }
        }
    }
}
