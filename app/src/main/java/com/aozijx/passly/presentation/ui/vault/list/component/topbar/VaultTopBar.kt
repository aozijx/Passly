package com.aozijx.passly.presentation.ui.vault.list.component.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListToolbarUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListContentUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListLayoutUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListNavigationUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenEventHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    uiState: VaultListToolbarUiModel,
    navigation: VaultListNavigationUiModel,
    content: VaultListContentUiModel,
    layout: VaultListLayoutUiModel,
    selectedQuickFilterIndex: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    eventHandler: VaultListScreenEventHandler,
) {
    val density = LocalDensity.current
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var navigateToSettingsAfterDismiss by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            isMoreMenuExpanded = false
            navigateToSettingsAfterDismiss = false
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(navigateToSettingsAfterDismiss, isMoreMenuExpanded) {
        if (navigateToSettingsAfterDismiss && !isMoreMenuExpanded) {
            navigateToSettingsAfterDismiss = false
            eventHandler.onSettingsClick()
        }
    }

    LaunchedEffect(
        layout.collapseTopBarOnScroll,
        layout.collapseQuickFilterBarOnScroll,
        layout.hideSystemBars,
    ) {
        if (!layout.collapseTopBarOnScroll &&
            (layout.collapseQuickFilterBarOnScroll || layout.hideSystemBars)
        ) {
            scrollBehavior.state.heightOffsetLimit = with(density) { -64.dp.toPx() }
        }
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column {
        CenterAlignedTopAppBar(
            scrollBehavior = if (layout.collapseTopBarOnScroll) scrollBehavior else null,
            windowInsets = WindowInsets.statusBars,
            title = {
                if (uiState.isSearchActive) {
                    VaultSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = eventHandler::onSearchQueryChanged,
                        focusRequester = focusRequester,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasCategoryFilter = !uiState.selectedCategory.isNullOrBlank()
                        Text(
                            text = when {
                                hasCategoryFilter -> stringResource(
                                    R.string.vault_title_category,
                                    uiState.selectedCategory
                                )
                                else -> stringResource(R.string.app_name)
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (hasCategoryFilter) {
                            IconButton(onClick = eventHandler::onClearCategory) {
                                Icon(
                                    Icons.Default.Clear,
                                    stringResource(R.string.vault_clear_filter),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { eventHandler.onSearchToggled(!uiState.isSearchActive) }) {
                    Icon(
                        if (uiState.isSearchActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                        contentDescription = stringResource(if (uiState.isSearchActive) R.string.back else R.string.search)
                    )
                }
            },
            actions = {
                if (!uiState.isSearchActive) {
                    Box {
                        IconButton(onClick = { isMoreMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }
                        if (isMoreMenuExpanded) {
                            VaultDropdownMenu(
                                onDismissRequest = { isMoreMenuExpanded = false },
                                showTOTPCode = content.showTotpCode,
                                onToggleTotpVisibility = eventHandler::onToggleTotpVisibility,
                                onSettingsClick = {
                                    navigateToSettingsAfterDismiss = true
                                },
                                availableCategories = uiState.availableCategories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = eventHandler::onCategorySelected,
                                selectedSort = uiState.selectedSort,
                                onSortSelected = eventHandler::onSortSelected,
                            )
                        }
                    }
                }
            })

        AnimatedVisibility(
            visible = navigation.visibleQuickFilters.size > 1 &&
                !uiState.isSearchActive &&
                uiState.selectedCategory == null &&
                    (!layout.collapseQuickFilterBarOnScroll ||
                        scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LibraryQuickFilterBar(
                quickFilters = navigation.visibleQuickFilters,
                selectedQuickFilterIndex = selectedQuickFilterIndex,
                onQuickFilterSelected = { index ->
                    navigation.visibleQuickFilters.getOrNull(index)
                        ?.let(eventHandler::onQuickFilterSelected)
                }
            )
        }
    }
}
