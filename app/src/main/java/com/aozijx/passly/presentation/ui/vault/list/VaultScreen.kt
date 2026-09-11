package com.aozijx.passly.presentation.ui.vault.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.vault.list.component.dialog.VaultDialogs
import com.aozijx.passly.presentation.ui.vault.list.component.fab.VaultFab
import com.aozijx.passly.presentation.ui.vault.list.component.list.VaultListContent
import com.aozijx.passly.presentation.ui.vault.list.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    itemEventHandler: VaultListItemEventHandler,
    otpStateProvider: VaultOtpStateProvider,
    fabScrollConnection: NestedScrollConnection,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    eventHandler: VaultListEventHandler,
) {
    val gridState = rememberLazyGridState()
    val pullThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val pullState = remember(pullThresholdPx, eventHandler) {
        PullToSearchGestureState(pullThresholdPx) {
            eventHandler.onEvent(VaultListEvent.SearchToggled(true))
        }
    }
    val pullToSearchConnection = remember(gridState, pullState, state.toolbar.isSearchActive) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!state.toolbar.isSearchActive) {
                    pullState.onPull(available.y, isAtTop = !gridState.canScrollBackward)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullState.reset()
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = state.toolbar.isSearchActive) {
        eventHandler.onEvent(VaultListEvent.SearchToggled(false))
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
        VaultListContent(
            content = state.content,
            entries = entries,
            itemEventHandler = itemEventHandler,
            otpStateProvider = otpStateProvider,
            gridState = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToSearchConnection),
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

internal class PullToSearchGestureState(
    private val thresholdPx: Float,
    private val onTriggered: () -> Unit,
) {
    private var distance = 0f
    private var triggered = false

    fun onPull(deltaY: Float, isAtTop: Boolean) {
        if (!isAtTop || deltaY <= 0f) {
            if (!triggered) distance = 0f
            return
        }
        if (triggered) return
        distance += deltaY
        if (distance >= thresholdPx) {
            triggered = true
            onTriggered()
        }
    }

    fun reset() {
        distance = 0f
        triggered = false
    }
}
