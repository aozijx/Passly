package com.aozijx.passly.presentation.feature.vault.list.ui.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.PagingData
import com.aozijx.passly.presentation.feature.vault.list.VaultUiAction
import com.aozijx.passly.presentation.feature.vault.list.toFeatureModel
import com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar.VaultFilterBar
import com.aozijx.passly.presentation.feature.vault.list.ui.gesture.rememberPullToSearchNestedScrollConnection
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemAction
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpStateProvider
import kotlinx.coroutines.flow.Flow

@Composable
internal fun VaultListBody(
    state: VaultListScreenUiModel,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    onItemAction: (VaultListItemAction) -> Unit,
    otpStateProvider: VaultOtpStateProvider,
    onAction: (VaultUiAction) -> Unit,
    onPullSearchProgressChanged: (Float) -> Unit,
    onSearchRequested: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val mainGridState = rememberLazyGridState()
    val searchGridState = remember(state.toolbar.isSearchActive) { LazyGridState() }
    val gridState = if (state.toolbar.isSearchActive) searchGridState else mainGridState
    val pullToSearchConnection = rememberPullToSearchNestedScrollConnection(
        gridState = gridState,
        enabled = state.content.isPullToSearchEnabled && !state.toolbar.isSearchActive,
        onProgressChanged = onPullSearchProgressChanged,
        onTriggered = onSearchRequested,
    )
    val hasActiveQuickFilters = state.navigation.selectedFilters.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        AnimatedVisibility(
            visible = !state.toolbar.isSearchActive &&
                    state.toolbar.selectedCategory == null &&
                    (hasActiveQuickFilters ||
                            !state.layout.collapseQuickFilterBarOnScroll ||
                            scrollBehavior.state.collapsedFraction < 0.5f),
            enter = expandVertically(animationSpec = motionScheme.defaultSpatialSpec()),
            exit = shrinkVertically(animationSpec = motionScheme.defaultSpatialSpec()),
        ) {
            VaultFilterBar(
                filters = state.navigation.filterOptions,
                selectedFilters = state.navigation.selectedFilters,
                onFilterToggled = { filter ->
                    onAction(VaultUiAction.FilterToggled(filter?.toFeatureModel()))
                },
            )
        }
        VaultEntryGrid(
            content = state.content,
            entries = entries,
            onItemAction = onItemAction,
            otpStateProvider = otpStateProvider,
            gridState = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullToSearchConnection),
        )
    }
}
