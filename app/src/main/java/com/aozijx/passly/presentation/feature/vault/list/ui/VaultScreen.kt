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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.PagingData
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.ui.component.fab.VaultFab
import com.aozijx.passly.presentation.feature.vault.list.ui.component.list.VaultListBody
import com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar.VaultTopBarUiState
import com.aozijx.passly.presentation.feature.vault.list.ui.gesture.rememberFabVisibilityNestedScrollConnection
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemAction
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.feature.vault.list.ui.search.rememberVaultSearchStateHolder
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
    onSettingsClick: () -> Unit,
    onAddTypeSelected: (VaultAddTypeUiModel) -> Unit,
) {
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val fabVisibilityConnection = rememberFabVisibilityNestedScrollConnection {
        isFabVisible = it
    }

    fun expandVaultBars() {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    val searchStateHolder = rememberVaultSearchStateHolder(
        searchActive = state.toolbar.isSearchActive,
        query = state.toolbar.searchQuery,
        onQueryChange = { onAction(VaultUiAction.SearchQueryChanged(it)) },
        onSearchActiveChange = { onAction(VaultUiAction.SearchToggled(it)) },
        onExpandBars = ::expandVaultBars,
    )

    LaunchedEffect(state.toolbar.isSearchActive, state.toolbar.searchQuery) {
        searchStateHolder.synchronize(
            searchActive = state.toolbar.isSearchActive,
            query = state.toolbar.searchQuery,
        )
    }

    BackHandler(enabled = state.toolbar.isSearchActive) {
        searchStateHolder.handleBack()
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { searchStateHolder.pause() }
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
                uiState = VaultTopBarUiState(
                    query = state.toolbar.searchQuery,
                    showTotpCode = state.content.showTotpCode,
                    selectedCategory = state.toolbar.selectedCategory,
                    selectedSort = state.toolbar.selectedSort,
                    availableCategories = state.toolbar.availableCategories,
                    collapseOnScroll = state.layout.collapseTopBarOnScroll,
                    collapseQuickFilterOnScroll = state.layout.collapseQuickFilterBarOnScroll,
                    hideSystemBars = state.layout.hideSystemBars,
                ),
                searchStateHolder = searchStateHolder,
                scrollBehavior = scrollBehavior,
                onAction = onAction,
                onSettingsClick = onSettingsClick,
            )
        },
        floatingActionButton = {
            VaultFab(
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
            scrollBehavior = scrollBehavior,
            entries = entries,
            onItemAction = onItemAction,
            otpStateProvider = otpStateProvider,
            onAction = onAction,
            onPullSearchProgressChanged = { progress ->
                searchStateHolder.updatePullProgress(progress)
            },
            onSearchRequested = searchStateHolder::request,
            contentPadding = padding,
        )
    }
}
