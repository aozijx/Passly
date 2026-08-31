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
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entryPages: Map<VaultQuickFilterUiModel, Flow<PagingData<VaultListItemUiModel>>>,
    itemEventHandler: VaultListItemEventHandler,
    otpStateProvider: VaultOtpStateProvider,
    fabScrollConnection: NestedScrollConnection,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    eventHandler: VaultListEventHandler,
) {
    val navigation = state.navigation
    val initialIndex = navigation.visibleQuickFilters.indexOf(navigation.selectedQuickFilter).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) {
        navigation.visibleQuickFilters.size.coerceAtLeast(1)
    }

    BackHandler(enabled = state.toolbar.isSearchActive) {
        eventHandler.onEvent(VaultListEvent.SearchToggled(false))
    }

    LaunchedEffect(navigation.visibleQuickFilters, navigation.selectedQuickFilter) {
        if (navigation.visibleQuickFilters.isEmpty()) return@LaunchedEffect
        if (navigation.selectedQuickFilter !in navigation.visibleQuickFilters) {
            eventHandler.onEvent(
                VaultListEvent.QuickFilterSelected(navigation.visibleQuickFilters.first()),
            )
            return@LaunchedEffect
        }
        val target = navigation.visibleQuickFilters.indexOf(navigation.selectedQuickFilter)
        if (pagerState.settledPage != target && pagerState.pageCount > target) {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState, navigation.visibleQuickFilters) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            navigation.visibleQuickFilters.getOrNull(page)?.let { filter ->
                eventHandler.onEvent(VaultListEvent.QuickFilterSelected(filter))
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
            .nestedScroll(fabScrollConnection),
        topBar = {
            Column {
                VaultTopBar(
                    uiState = state.toolbar,
                    navigation = state.navigation,
                    content = state.content,
                    layout = state.layout,
                    selectedQuickFilterIndex = pagerState.currentPage,
                    scrollBehavior = scrollBehavior,
                    eventHandler = eventHandler,
                )
                if (state.layout.isDatabaseInitializing) {
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
                onAddTypeSelected = { type ->
                    eventHandler.onEvent(VaultListEvent.AddTypeSelected(type))
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isVisible = state.layout.isFabVisible,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        VaultPagerContent(
            pagerState = pagerState,
            navigation = state.navigation,
            content = state.content,
            entryPages = entryPages,
            itemEventHandler = itemEventHandler,
            otpStateProvider = otpStateProvider,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }

    VaultDialogs(
        uiState = state.dialogs,
        onDismissAddType = { eventHandler.onEvent(VaultListEvent.DismissAddType) },
        onConfirmDelete = { eventHandler.onEvent(VaultListEvent.ConfirmDelete) },
        onDismissDelete = { eventHandler.onEvent(VaultListEvent.DismissDelete) },
        requestAuthentication = eventHandler::requestAuthentication,
    )
}
