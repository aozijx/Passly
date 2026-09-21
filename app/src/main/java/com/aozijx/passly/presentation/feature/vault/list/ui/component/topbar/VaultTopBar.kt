package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEvent
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEventHandler
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListLayoutUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListToolbarUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchPhase
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchState
import com.aozijx.passly.presentation.ui.shared.components.topbar.passlyCompactTopAppBarColors
import com.aozijx.passly.presentation.ui.shared.components.topbar.topAppBarContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    uiState: VaultListToolbarUiModel,
    content: VaultListContentUiModel,
    layout: VaultListLayoutUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    searchState: VaultSearchState,
    onSearchFocusChanged: (Boolean) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchExitRequested: () -> Unit,
    eventHandler: VaultListEventHandler,
) {
    val density = LocalDensity.current
    val motionScheme = MaterialTheme.motionScheme
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var navigateToSettingsAfterDismiss by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            isMoreMenuExpanded = false
            navigateToSettingsAfterDismiss = false
        }
    }

    val expansionProgress by animateFloatAsState(
        targetValue = searchState.layoutProgress,
        animationSpec = if (searchState.isDirectManipulation) {
            snap()
        } else {
            motionScheme.defaultEffectsSpec()
        },
        label = "VaultSearchExpansion",
    )
    val searchWidthFraction = 0.94f + (0.06f * expansionProgress.coerceIn(0f, 1f))
    val topBarContainerColor = topAppBarContainerColor(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrollProgress = scrollBehavior.state.overlappedFraction,
    )

    LaunchedEffect(navigateToSettingsAfterDismiss, isMoreMenuExpanded) {
        if (navigateToSettingsAfterDismiss && !isMoreMenuExpanded) {
            navigateToSettingsAfterDismiss = false
            eventHandler.onEvent(VaultListEvent.SettingsClicked)
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

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = motionScheme.defaultEffectsSpec(),
        ),
    ) {
        TopAppBar(
            modifier = Modifier.background(topBarContainerColor),
            scrollBehavior = if (layout.collapseTopBarOnScroll && !searchState.isEditing) {
                scrollBehavior
            } else {
                null
            },
            windowInsets = WindowInsets.statusBars,
            colors = passlyCompactTopAppBarColors(),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    VaultSearchBar(
                        query = uiState.searchQuery,
                        isEditing = searchState.isEditing,
                        onQueryChange = { query ->
                            eventHandler.onEvent(VaultListEvent.SearchQueryChanged(query))
                        },
                        onSearch = onSearchSubmitted,
                        onFocusChanged = onSearchFocusChanged,
                        trailingIcon = {
                            AnimatedContent(
                                targetState = searchState.phase.takeUnless {
                                    it == VaultSearchPhase.PULLING
                                } ?: VaultSearchPhase.BROWSING,
                                transitionSpec = {
                                    fadeIn(motionScheme.fastEffectsSpec()) togetherWith
                                            fadeOut(motionScheme.fastEffectsSpec())
                                },
                                label = "VaultSearchAction",
                            ) { phase ->
                                if (phase == VaultSearchPhase.BROWSING ||
                                    phase == VaultSearchPhase.PULLING
                                ) {
                                    Box {
                                        IconButton(onClick = { isMoreMenuExpanded = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                stringResource(R.string.more),
                                            )
                                        }
                                        if (isMoreMenuExpanded) {
                                            VaultDropdownMenu(
                                                onDismissRequest = { isMoreMenuExpanded = false },
                                                showTOTPCode = content.showTotpCode,
                                                onToggleTotpVisibility = {
                                                    eventHandler.onEvent(
                                                        VaultListEvent.ToggleTotpVisibility,
                                                    )
                                                },
                                                onSettingsClick = {
                                                    navigateToSettingsAfterDismiss = true
                                                },
                                                availableCategories = uiState.availableCategories,
                                                selectedCategory = uiState.selectedCategory,
                                                onCategorySelected = { category ->
                                                    eventHandler.onEvent(
                                                        VaultListEvent.CategorySelected(category),
                                                    )
                                                },
                                                selectedSort = uiState.selectedSort,
                                                onSortSelected = { sort ->
                                                    eventHandler.onEvent(
                                                        VaultListEvent.SortSelected(sort),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                } else if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            eventHandler.onEvent(
                                                VaultListEvent.SearchQueryChanged(""),
                                            )
                                            if (phase == VaultSearchPhase.RESULTS) {
                                                onSearchExitRequested()
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            stringResource(R.string.vault_clear_filter),
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(searchWidthFraction),
                    )
                }
            },
        )
        uiState.selectedCategory?.takeIf(String::isNotBlank)?.let { category ->
            InputChip(
                selected = true,
                onClick = { eventHandler.onEvent(VaultListEvent.ClearCategory) },
                label = { Text(category) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.vault_clear_filter),
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
