package com.aozijx.passly.presentation.ui.vault.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.vault.list.component.dialog.VaultDialogs
import com.aozijx.passly.presentation.ui.vault.list.component.fab.VaultFab
import com.aozijx.passly.presentation.ui.vault.list.component.list.VaultPagerContent
import com.aozijx.passly.presentation.ui.vault.list.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.ui.vault.list.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSortUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entryPages: Map<VaultQuickFilterUiModel, Flow<PagingData<VaultListItemUiModel>>>,
    cardPresentations: List<VaultCardPresentationUiModel>,
    otpState: (String) -> Flow<VaultOtpUiState?>,
    swipeLeftAction: VaultSwipeActionUiModel,
    swipeRightAction: VaultSwipeActionUiModel,
    isSwipeEnabled: Boolean,
    fabScrollConnection: NestedScrollConnection,
    isFabVisible: Boolean,
    collapseTopBarOnScroll: Boolean,
    collapseQuickFilterBarOnScroll: Boolean,
    hideSystemBars: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggled: (Boolean) -> Unit,
    onClearCategory: () -> Unit,
    onToggleTotpVisibility: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (VaultSortUiModel) -> Unit,
    onQuickFilterSelected: (VaultQuickFilterUiModel) -> Unit,
    onAddTypeSelected: (VaultAddTypeUiModel) -> Unit,
    onDismissAddType: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    isDatabaseInitializing: Boolean,
) {
    val initialIndex = state.visibleQuickFilters.indexOf(state.selectedQuickFilter).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) {
        state.visibleQuickFilters.size.coerceAtLeast(1)
    }

    BackHandler(enabled = state.isSearchActive) { onSearchToggled(false) }

    LaunchedEffect(state.visibleQuickFilters, state.selectedQuickFilter) {
        if (state.visibleQuickFilters.isEmpty()) return@LaunchedEffect
        if (state.selectedQuickFilter !in state.visibleQuickFilters) {
            onQuickFilterSelected(state.visibleQuickFilters.first())
            return@LaunchedEffect
        }
        val target = state.visibleQuickFilters.indexOf(state.selectedQuickFilter)
        if (pagerState.settledPage != target && pagerState.pageCount > target) {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState, state.visibleQuickFilters) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            state.visibleQuickFilters.getOrNull(page)?.let(onQuickFilterSelected)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (collapseTopBarOnScroll || collapseQuickFilterBarOnScroll || hideSystemBars) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            )
            .nestedScroll(fabScrollConnection),
        topBar = {
            Column {
                VaultTopBar(
                    uiState = state,
                    selectedQuickFilterIndex = pagerState.currentPage,
                    scrollBehavior = scrollBehavior,
                    onSettingsClick = onSettingsClick,
                    isStatusBarAutoHide = hideSystemBars,
                    isTopBarCollapsible = collapseTopBarOnScroll,
                    isQuickFilterBarCollapsible = collapseQuickFilterBarOnScroll,
                    onSearchQueryChange = onSearchQueryChange,
                    onToggleSearch = onSearchToggled,
                    onClearCategory = onClearCategory,
                    onToggleTotpVisibility = onToggleTotpVisibility,
                    onCategorySelected = onCategorySelected,
                    onSortSelected = onSortSelected,
                    onSelectQuickFilter = onQuickFilterSelected,
                )
                if (isDatabaseInitializing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        },
        floatingActionButton = {
            VaultFab(
                onAddTypeSelected = onAddTypeSelected,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isVisible = isFabVisible,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        VaultPagerContent(
            pagerState = pagerState,
            uiState = state,
            entryPages = entryPages,
            entryCardPresentations = cardPresentations,
            otpState = otpState,
            swipeLeftAction = swipeLeftAction,
            swipeRightAction = swipeRightAction,
            isSwipeEnabled = isSwipeEnabled,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }

    VaultDialogs(
        uiState = state,
        onDismissAddType = onDismissAddType,
        onConfirmDelete = onConfirmDelete,
        onDismissDelete = onDismissDelete,
        requestAuthentication = requestAuthentication,
    )
}
