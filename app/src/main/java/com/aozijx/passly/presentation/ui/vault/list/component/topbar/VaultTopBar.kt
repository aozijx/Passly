package com.aozijx.passly.presentation.ui.vault.list.component.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListContentUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListLayoutUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListToolbarUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopBar(
    uiState: VaultListToolbarUiModel,
    content: VaultListContentUiModel,
    layout: VaultListLayoutUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    isSearchEditing: Boolean,
    pullSearchProgress: Float,
    onSearchEditingChanged: (Boolean) -> Unit,
    eventHandler: VaultListEventHandler,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    var navigateToSettingsAfterDismiss by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            isMoreMenuExpanded = false
            navigateToSettingsAfterDismiss = false
        }
    }

    val searchMode = resolveVaultSearchBarMode(
        isSearchActive = uiState.isSearchActive,
        isEditing = isSearchEditing,
        query = uiState.searchQuery,
    )
    val expansionTarget = resolveVaultSearchBarExpansion(
        pullProgress = pullSearchProgress,
        mode = searchMode,
    )
    val expansionProgress by animateFloatAsState(
        targetValue = expansionTarget,
        animationSpec = if (pullSearchProgress > 0f) {
            tween(durationMillis = 70, easing = LinearOutSlowInEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "VaultSearchExpansion",
    )
    val horizontalInset = lerp(10.dp, 0.dp, expansionProgress)
    val verticalOffset = lerp(0.dp, 4.dp, expansionProgress)

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

    LaunchedEffect(isSearchEditing) {
        if (isSearchEditing) {
            scrollBehavior.state.heightOffset = 0f
        } else {
            focusManager.clearFocus()
        }
    }

    Column(modifier = Modifier.animateContentSize()) {
        TopAppBar(
            scrollBehavior = if (layout.collapseTopBarOnScroll && !isSearchEditing) {
                scrollBehavior
            } else {
                null
            },
            windowInsets = WindowInsets.statusBars,
            title = {
                VaultSearchBar(
                    query = uiState.searchQuery,
                    mode = searchMode,
                    onQueryChange = { query ->
                        eventHandler.onEvent(VaultListEvent.SearchQueryChanged(query))
                    },
                    onSearch = { query ->
                        onSearchEditingChanged(false)
                        if (query.isBlank()) {
                            eventHandler.onEvent(VaultListEvent.SearchToggled(false))
                        }
                    },
                    onEditingChange = { editing ->
                        onSearchEditingChanged(editing)
                        if (editing && !uiState.isSearchActive) {
                            eventHandler.onEvent(VaultListEvent.SearchToggled(true))
                        }
                    },
                    trailingIcon = {
                        AnimatedContent(
                            targetState = searchMode,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "VaultSearchAction",
                        ) { mode ->
                            if (mode == VaultSearchBarMode.DEFAULT) {
                                Box {
                                    IconButton(onClick = { isMoreMenuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, stringResource(R.string.more))
                                    }
                                    if (isMoreMenuExpanded) {
                                        VaultDropdownMenu(
                                            onDismissRequest = { isMoreMenuExpanded = false },
                                            showTOTPCode = content.showTotpCode,
                                            onToggleTotpVisibility = {
                                                eventHandler.onEvent(VaultListEvent.ToggleTotpVisibility)
                                            },
                                            onSettingsClick = {
                                                navigateToSettingsAfterDismiss = true
                                            },
                                            availableCategories = uiState.availableCategories,
                                            selectedCategory = uiState.selectedCategory,
                                            onCategorySelected = { category ->
                                                eventHandler.onEvent(
                                                    VaultListEvent.CategorySelected(
                                                        category
                                                    )
                                                )
                                            },
                                            selectedSort = uiState.selectedSort,
                                            onSortSelected = { sort ->
                                                eventHandler.onEvent(
                                                    VaultListEvent.SortSelected(
                                                        sort
                                                    )
                                                )
                                            },
                                        )
                                    }
                                }
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        eventHandler.onEvent(VaultListEvent.SearchQueryChanged(""))
                                        if (mode == VaultSearchBarMode.RESULT) {
                                            eventHandler.onEvent(VaultListEvent.SearchToggled(false))
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        stringResource(R.string.vault_clear_filter)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        onSearchEditingChanged(false)
                                        eventHandler.onEvent(VaultListEvent.SearchToggled(false))
                                    },
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        stringResource(R.string.back),
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = horizontalInset)
                        .offset(y = verticalOffset),
                )
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
