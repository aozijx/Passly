package com.aozijx.passly.presentation.ui.vault.list.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.PagingData
import com.aozijx.passly.presentation.ui.vault.list.component.topbar.VaultFilterBar
import com.aozijx.passly.presentation.ui.vault.list.gesture.rememberPullToSearchNestedScrollConnection
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEvent
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import kotlinx.coroutines.flow.Flow

@Composable
internal fun VaultListBody(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    itemEventHandler: VaultListItemEventHandler,
    otpStateProvider: VaultOtpStateProvider,
    eventHandler: VaultListEventHandler,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val pullToSearchConnection = rememberPullToSearchNestedScrollConnection(
        gridState = gridState,
        enabled = !state.toolbar.isSearchActive,
        onTriggered = { eventHandler.onEvent(VaultListEvent.SearchToggled(true)) },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        AnimatedVisibility(
            visible = !state.toolbar.isSearchActive &&
                    state.toolbar.selectedCategory == null &&
                    (!state.layout.collapseQuickFilterBarOnScroll ||
                            scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            VaultFilterBar(
                filters = state.navigation.filterOptions,
                selectedFilters = state.navigation.selectedFilters,
                onFilterToggled = { filter ->
                    eventHandler.onEvent(VaultListEvent.FilterToggled(filter))
                },
            )
        }
        VaultEntryGrid(
            content = state.content,
            entries = entries,
            itemEventHandler = itemEventHandler,
            otpStateProvider = otpStateProvider,
            gridState = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullToSearchConnection),
        )
    }
}
