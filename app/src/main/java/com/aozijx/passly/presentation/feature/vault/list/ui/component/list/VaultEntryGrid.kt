package com.aozijx.passly.presentation.feature.vault.list.ui.component.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultOtpStateProvider
import kotlinx.coroutines.flow.Flow

@Composable
internal fun VaultEntryGrid(
    content: VaultListContentUiModel,
    entries: Flow<PagingData<VaultListItemUiModel>>,
    itemEventHandler: VaultListItemEventHandler,
    otpStateProvider: VaultOtpStateProvider,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val motionScheme = MaterialTheme.motionScheme
    var playInitialEntryAnimation by rememberSaveable { mutableStateOf(true) }
    val pagingItems = entries.collectAsLazyPagingItems()
    val refreshState = pagingItems.loadState.refresh

    LaunchedEffect(refreshState, pagingItems.itemCount) {
        if (refreshState !is LoadState.Loading && pagingItems.itemCount > 0) {
            playInitialEntryAnimation = false
        }
    }

    Box(modifier = modifier) {
        when {
            refreshState is LoadState.Loading && pagingItems.itemCount == 0 ->
                VaultPagingProgress()

            refreshState is LoadState.Error && pagingItems.itemCount == 0 ->
                VaultPagingError(onRetry = pagingItems::retry)

            pagingItems.itemCount == 0 -> EmptyVaultPlaceholder()

            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(
                    minSize = if (adaptiveLayout.isExpanded) 360.dp else 440.dp,
                ),
                modifier = Modifier.matchParentSize(),
                contentPadding = PaddingValues(
                    horizontal = if (adaptiveLayout.isAtLeastMedium) 24.dp else 16.dp,
                    vertical = 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey(VaultListItemUiModel::id),
                    contentType = pagingItems.itemContentType(VaultListItemUiModel::entryType),
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    VaultEntryRow(
                        item = item,
                        eventHandler = itemEventHandler,
                        content = content,
                        otpStateProvider = otpStateProvider,
                        animateInitialAppearance = playInitialEntryAnimation,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = motionScheme.defaultSpatialSpec(),
                                fadeOutSpec = motionScheme.fastEffectsSpec(),
                            )
                            .fillMaxWidth(),
                    )
                }

                when (pagingItems.loadState.append) {
                    is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        VaultPagingProgress(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                        )
                    }

                    is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                        VaultPagingError(
                            onRetry = pagingItems::retry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                        )
                    }

                    is LoadState.NotLoading -> Unit
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(
                        modifier = Modifier
                            .height(60.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultPagingProgress(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VaultPagingError(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.vault_load_failed))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
