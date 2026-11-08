package com.aozijx.passly.feature.vault.presentation.list.ui.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import com.aozijx.passly.feature.vault.presentation.list.VaultUiAction
import com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar.VaultFilterBar
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemAction
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListScreenUiModel
import com.aozijx.passly.feature.vault.presentation.list.VaultOtpStateProvider
import kotlinx.coroutines.flow.Flow

@Composable
internal fun VaultListBody(
    state: VaultListScreenUiModel,
    gridState: LazyGridState,
    scrollBehavior: TopAppBarScrollBehavior,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    onItemAction: (VaultListItemAction) -> Unit,
    otpStateProvider: VaultOtpStateProvider,
    onAction: (VaultUiAction) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding),
    ) {
        AnimatedVisibility(
            visible =
                !state.toolbar.isSearchActive &&
                    state.toolbar.selectedCategory == null &&
                    (
                        state.navigation.hasActiveFilters ||
                            !state.layout.collapseQuickFilterBarOnScroll ||
                            scrollBehavior.state.collapsedFraction < 0.5f
                    ),
            enter = expandVertically(animationSpec = motionScheme.defaultSpatialSpec()),
            exit = shrinkVertically(animationSpec = motionScheme.defaultSpatialSpec()),
        ) {
            VaultFilterBar(
                filters = state.navigation.filters,
                allFiltersSelected = state.navigation.allFiltersSelected,
                onFilterToggled = { filter ->
                    onAction(VaultUiAction.FilterToggled(filter?.featureType))
                },
            )
        }
        VaultEntryGrid(
            content = state.content,
            entries = entries,
            onItemAction = onItemAction,
            otpStateProvider = otpStateProvider,
            gridState = gridState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )
    }
}
