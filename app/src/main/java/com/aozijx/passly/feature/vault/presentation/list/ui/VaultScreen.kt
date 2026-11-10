package com.aozijx.passly.feature.vault.presentation.list.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.PagingData
import com.aozijx.passly.feature.vault.presentation.list.VaultRouteRequest
import com.aozijx.passly.feature.vault.presentation.list.VaultUiAction
import com.aozijx.passly.feature.vault.presentation.list.ui.component.fab.VaultFab
import com.aozijx.passly.feature.vault.presentation.list.ui.component.list.VaultListBody
import com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar.VaultMenuUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar.VaultTopBar
import com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar.VaultTopBarUiState
import com.aozijx.passly.feature.vault.presentation.list.ui.gesture.OrderedNestedScrollConnection
import com.aozijx.passly.feature.vault.presentation.list.ui.gesture.rememberFabVisibilityNestedScrollConnection
import com.aozijx.passly.feature.vault.presentation.list.ui.gesture.rememberPullToSearchNestedScrollConnection
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemAction
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.feature.vault.presentation.list.VaultOtpStateProvider
import com.aozijx.passly.feature.vault.presentation.list.ui.search.rememberVaultSearchStateHolder
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    onItemAction: (VaultListItemAction) -> Unit,
    otpStateProvider: VaultOtpStateProvider,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAction: (VaultUiAction) -> Unit,
    onRouteRequest: (VaultRouteRequest) -> Unit,
    onAddTypeSelected: (VaultAddTypeUiModel) -> Unit,
) {
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val fabVisibilityConnection =
        rememberFabVisibilityNestedScrollConnection {
            isFabVisible = it
        }

    fun expandVaultBars() {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    val searchStateHolder =
        rememberVaultSearchStateHolder(
            search = state.toolbar.search,
            onQueryChange = { onAction(VaultUiAction.SearchQueryChanged(it)) },
            onSearchActiveChange = { onAction(VaultUiAction.SearchToggled(it)) },
            onExpandBars = ::expandVaultBars,
        )
    val mainGridState = rememberLazyGridState()
    val searchGridState = remember(state.toolbar.search.isActive) { LazyGridState() }
    val gridState = if (state.toolbar.search.isActive) searchGridState else mainGridState
    val pullToSearchConnection =
        rememberPullToSearchNestedScrollConnection(
            gridState = gridState,
            enabled = state.content.isPullToSearchEnabled && !state.toolbar.search.isActive,
            onProgressChanged = searchStateHolder::updatePullProgress,
            onTriggered = searchStateHolder::request,
        )
    val appBarConsumesScroll =
        state.layout.collapseTopBarOnScroll ||
            state.layout.collapseQuickFilterBarOnScroll || state.layout.hideSystemBars
    val vaultScrollConnection =
        if (appBarConsumesScroll) {
            remember(pullToSearchConnection, scrollBehavior.nestedScrollConnection) {
                OrderedNestedScrollConnection(
                    listOf(
                        pullToSearchConnection,
                        scrollBehavior.nestedScrollConnection,
                    ),
                )
            }
        } else {
            pullToSearchConnection
        }

    LaunchedEffect(state.toolbar.search) {
        searchStateHolder.synchronize(state.toolbar.search)
    }

    BackHandler(enabled = state.toolbar.search.isActive) {
        searchStateHolder.handleBack()
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { searchStateHolder.pause() }
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(vaultScrollConnection)
                .nestedScroll(fabVisibilityConnection),
        topBar = {
            VaultTopBar(
                uiState =
                    VaultTopBarUiState(
                        search = state.toolbar.search,
                        menu =
                            VaultMenuUiState(
                                showTotpCode = state.content.showTotpCode,
                                categorySearchQuery = state.toolbar.categorySearchQuery,
                                matchingCategories = state.toolbar.matchingCategories,
                                categoriesState = state.toolbar.categoriesState,
                                categorySelection = state.toolbar.categorySelection,
                                selectedSort = state.toolbar.selectedSort,
                                sortOptions = state.toolbar.sortOptions,
                            ),
                        collapseOnScroll = state.layout.collapseTopBarOnScroll,
                        collapseQuickFilterOnScroll = state.layout.collapseQuickFilterBarOnScroll,
                        hideSystemBars = state.layout.hideSystemBars,
                    ),
                searchStateHolder = searchStateHolder,
                scrollBehavior = scrollBehavior,
                onAction = onAction,
                onRouteRequest = onRouteRequest,
            )
        },
        floatingActionButton = {
            VaultFab(
                creationOptions = state.navigation.creationOptions,
                fabMenuOptions = state.navigation.fabMenuOptions,
                onAddTypeSelected = { type ->
                    onAddTypeSelected(type)
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isVisible = isFabVisible,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        VaultListBody(
            state = state,
            gridState = gridState,
            scrollBehavior = scrollBehavior,
            entries = entries,
            onItemAction = onItemAction,
            otpStateProvider = otpStateProvider,
            onAction = onAction,
            contentPadding = padding,
        )
    }
}
