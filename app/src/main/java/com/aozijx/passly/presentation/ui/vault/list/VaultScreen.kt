package com.aozijx.passly.presentation.ui.vault.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.vault.list.component.fab.VaultFab
import com.aozijx.passly.presentation.ui.vault.list.component.list.VaultListBody
import com.aozijx.passly.presentation.ui.vault.list.component.topbar.VaultTopBar
import com.aozijx.passly.presentation.ui.vault.list.gesture.rememberFabVisibilityNestedScrollConnection
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
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    eventHandler: VaultListEventHandler,
) {
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val fabVisibilityConnection = rememberFabVisibilityNestedScrollConnection {
        isFabVisible = it
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
            .nestedScroll(fabVisibilityConnection),
        topBar = {
            VaultTopBar(
                uiState = state.toolbar,
                content = state.content,
                layout = state.layout,
                scrollBehavior = scrollBehavior,
                eventHandler = eventHandler,
            )
        },
        floatingActionButton = {
            VaultFab(
                onAddTypeSelected = { type ->
                    eventHandler.onEvent(VaultListEvent.AddTypeSelected(type))
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
            itemEventHandler = itemEventHandler,
            otpStateProvider = otpStateProvider,
            eventHandler = eventHandler,
            contentPadding = padding,
        )
    }
}
