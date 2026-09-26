package com.aozijx.passly.presentation.feature.vault.list.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.PagingData
import com.aozijx.passly.presentation.feature.vault.list.ui.component.fab.VaultFab
import com.aozijx.passly.presentation.feature.vault.list.ui.component.list.VaultListBody
import com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.feature.vault.list.ui.gesture.rememberFabVisibilityNestedScrollConnection
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListEvent
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemEvent
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchPhase
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    onItemEvent: (VaultListItemEvent) -> Unit,
    otpStateProvider: VaultOtpStateProvider,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onEvent: (VaultListEvent) -> Unit,
) {
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    var searchState by remember {
        mutableStateOf(
            VaultSearchState.initial(
                isSearchActive = state.toolbar.isSearchActive,
                query = state.toolbar.searchQuery,
            ),
        )
    }
    var wasSearchActive by remember { mutableStateOf(state.toolbar.isSearchActive) }
    val currentSearchQuery by rememberUpdatedState(state.toolbar.searchQuery)
    val currentSearchActive by rememberUpdatedState(state.toolbar.isSearchActive)
    val fabVisibilityConnection = rememberFabVisibilityNestedScrollConnection {
        isFabVisible = it
    }

    fun expandVaultBars() {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    LaunchedEffect(state.toolbar.isSearchActive, state.toolbar.searchQuery) {
        val searchExited = wasSearchActive && !state.toolbar.isSearchActive
        searchState = searchState.synchronize(
            isSearchActive = state.toolbar.isSearchActive,
            query = state.toolbar.searchQuery,
        )
        if (searchExited || searchState.isEditing) expandVaultBars()
        wasSearchActive = state.toolbar.isSearchActive
    }

    BackHandler(enabled = state.toolbar.isSearchActive) {
        if (searchState.isEditing) {
            searchState = searchState.settle(state.toolbar.searchQuery)
            if (state.toolbar.searchQuery.isBlank()) {
                onEvent(VaultListEvent.SearchToggled(false))
            }
        } else {
            searchState = searchState.synchronize(false, state.toolbar.searchQuery)
            expandVaultBars()
            onEvent(VaultListEvent.SearchToggled(false))
        }
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            searchState = searchState.onScreenPaused(currentSearchQuery)
            if (currentSearchActive && currentSearchQuery.isBlank()) {
                onEvent(VaultListEvent.SearchToggled(false))
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (state.layout.collapseTopBarOnScroll ||
                    state.layout.collapseQuickFilterBarOnScroll || state.layout.hideSystemBars
                ) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else Modifier
            )
            .nestedScroll(fabVisibilityConnection),
        topBar = {
            VaultTopBar(
                uiState = state.toolbar,
                content = state.content,
                layout = state.layout,
                scrollBehavior = scrollBehavior,
                searchState = searchState,
                onSearchFocusChanged = { focused ->
                    val nextState = searchState.onFocusChanged(
                        focused = focused,
                        query = state.toolbar.searchQuery,
                    )
                    searchState = nextState
                    if (focused && !state.toolbar.isSearchActive) {
                        expandVaultBars()
                        onEvent(VaultListEvent.SearchToggled(true))
                    } else if (!focused &&
                        nextState.phase == VaultSearchPhase.BROWSING &&
                        state.toolbar.isSearchActive
                    ) {
                        onEvent(VaultListEvent.SearchToggled(false))
                    }
                },
                onSearchSubmitted = { query ->
                    searchState = searchState.settle(query)
                    if (query.isBlank()) {
                        onEvent(VaultListEvent.SearchToggled(false))
                    }
                },
                onSearchExitRequested = {
                    searchState = searchState.synchronize(false, state.toolbar.searchQuery)
                    expandVaultBars()
                    onEvent(VaultListEvent.SearchToggled(false))
                },
                onEvent = onEvent,
            )
        },
        floatingActionButton = {
            VaultFab(
                onAddTypeSelected = { type ->
                    onEvent(VaultListEvent.AddTypeSelected(type))
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
            scrollBehavior = scrollBehavior,
            entries = entries,
            onItemEvent = onItemEvent,
            otpStateProvider = otpStateProvider,
            onEvent = onEvent,
            onPullSearchProgressChanged = { progress ->
                searchState = searchState.onPullProgressChanged(progress)
            },
            onSearchRequested = {
                searchState = searchState.startEditing()
                expandVaultBars()
                onEvent(VaultListEvent.SearchToggled(true))
            },
            contentPadding = padding,
        )
    }
}
