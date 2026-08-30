package com.aozijx.passly.presentation.ui.vault.list.component.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.core.ui.components.widgets.SwipeActionContainer
import com.aozijx.passly.core.ui.components.widgets.SwipeActionSpec
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.vault.list.component.cardstyle.CardStyleRegistry
import com.aozijx.passly.presentation.ui.vault.list.model.VaultCardPresentationUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListContentUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemEventHandler
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListNavigationUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpStateProvider
import com.aozijx.passly.presentation.ui.vault.list.model.VaultOtpUiState
import com.aozijx.passly.presentation.ui.vault.list.model.VaultQuickFilterUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun VaultPagerContent(
    pagerState: PagerState,
    navigation: VaultListNavigationUiModel,
    content: VaultListContentUiModel,
    entryPages: Map<VaultQuickFilterUiModel, Flow<PagingData<VaultListItemUiModel>>>,
    itemEventHandler: VaultListItemEventHandler,
    otpStateProvider: VaultOtpStateProvider,
    modifier: Modifier = Modifier
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val motionScheme = MaterialTheme.motionScheme
    var playInitialEntryAnimation by rememberSaveable { mutableStateOf(true) }

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = 1.coerceAtMost(
            (navigation.visibleQuickFilters.size - 1).coerceAtLeast(0)
        ),
        key = { pageIndex ->
            navigation.visibleQuickFilters.getOrNull(pageIndex)?.name ?: "vault-empty"
        }
    ) { pageIndex ->
        val currentQuickFilter =
            navigation.visibleQuickFilters.getOrNull(pageIndex) ?: VaultQuickFilterUiModel.ALL
        val isCurrentPage = pageIndex == pagerState.currentPage
        val pagingItems = requireNotNull(entryPages[currentQuickFilter]).collectAsLazyPagingItems()
        val refreshState = pagingItems.loadState.refresh

        LaunchedEffect(refreshState, pagingItems.itemCount) {
            if (refreshState !is LoadState.Loading && pagingItems.itemCount > 0) {
                playInitialEntryAnimation = false
            }
        }

        when {
            refreshState is LoadState.Loading && pagingItems.itemCount == 0 ->
                VaultPagingProgress()

            refreshState is LoadState.Error && pagingItems.itemCount == 0 ->
                VaultPagingError(onRetry = pagingItems::retry)

            pagingItems.itemCount == 0 -> EmptyVaultPlaceholder()

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(
                    minSize = if (adaptiveLayout.isExpanded) 360.dp else 440.dp
                ),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (adaptiveLayout.isAtLeastMedium) 24.dp else 16.dp,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey(VaultListItemUiModel::id),
                    contentType = pagingItems.itemContentType(VaultListItemUiModel::entryType),
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    EntryListItemRow(
                        item = item,
                        eventHandler = itemEventHandler,
                        entryCardPresentations = content.cardPresentations,
                        swipeLeftAction = content.swipeLeftAction,
                        swipeRightAction = content.swipeRightAction,
                        isSwipeEnabled = content.isSwipeEnabled,
                        otpStateProvider = otpStateProvider,
                        showTotpCode = content.showTotpCode,
                        isCurrentPage = isCurrentPage,
                        animateInitialAppearance = playInitialEntryAnimation,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = motionScheme.defaultSpatialSpec(),
                                fadeOutSpec = motionScheme.fastEffectsSpec()
                            )
                            .fillMaxWidth()
                    )
                }

                when (pagingItems.loadState.append) {
                    is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        VaultPagingProgress(modifier = Modifier.fillMaxWidth().height(64.dp))
                    }
                    is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                        VaultPagingError(
                            onRetry = pagingItems::retry,
                            modifier = Modifier.fillMaxWidth().height(96.dp),
                        )
                    }
                    is LoadState.NotLoading -> Unit
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(
                        modifier = Modifier
                            .height(60.dp)
                            .navigationBarsPadding()
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

@Composable
private fun EntryListItemRow(
    item: VaultListItemUiModel,
    eventHandler: VaultListItemEventHandler,
    entryCardPresentations: List<VaultCardPresentationUiModel>,
    swipeLeftAction: SwipeActionUiModel,
    swipeRightAction: SwipeActionUiModel,
    isSwipeEnabled: Boolean,
    otpStateProvider: VaultOtpStateProvider,
    showTotpCode: Boolean,
    isCurrentPage: Boolean,
    animateInitialAppearance: Boolean,
    modifier: Modifier = Modifier
) {
    val totpState = if (item.hasOtp) {
        observeVaultOtpState(
            entryId = item.id,
            provider = otpStateProvider,
            enabled = showTotpCode && isCurrentPage,
        )
    } else {
        null
    }
    val cardStyle = remember(item.entryType, item.hasPassword, item.hasOtp, entryCardPresentations) {
        CardStyleRegistry.resolveStyle(item, entryCardPresentations)
    }
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val visibleState = remember(item.id) {
        MutableTransitionState(!animateInitialAppearance).apply { targetState = true }
    }
    val currentItem by rememberUpdatedState(item)
    val leftAction =
        remember(item.id, swipeLeftAction, eventHandler, colorScheme) {
            createAppSwipeActionSpec(
                actionType = swipeLeftAction,
                onAction = { eventHandler.onSwipe(currentItem, swipeLeftAction) },
                backgroundColor = if (swipeLeftAction == SwipeActionUiModel.DELETE) colorScheme.error else colorScheme.primary,
                iconTint = Color.White
            )
        }
    val rightAction =
        remember(item.id, swipeRightAction, eventHandler, colorScheme) {
            createAppSwipeActionSpec(
                actionType = swipeRightAction,
                onAction = { eventHandler.onSwipe(currentItem, swipeRightAction) },
                backgroundColor = if (swipeRightAction == SwipeActionUiModel.DELETE) colorScheme.error else colorScheme.secondary,
                iconTint = Color.White
            )
        }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetY = { height -> height / 4 }
                )
    ) {
        SwipeActionContainer(
            leftAction = leftAction,
            rightAction = rightAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = isSwipeEnabled,
        ) {
            cardStyle.Render(
                entry = item,
                totpState = totpState,
                showTotpCode = showTotpCode,
                onClick = { eventHandler.onClick(item) }
            )
        }
    }
}

@Composable
internal fun observeVaultOtpState(
    entryId: String,
    provider: VaultOtpStateProvider,
    enabled: Boolean,
): VaultOtpUiState? {
    if (!enabled) return null
    DisposableEffect(entryId, provider) {
        provider.subscribe(entryId)
        onDispose { provider.unsubscribe(entryId) }
    }
    val stateFlow = remember(entryId, provider) {
        provider.state(entryId).distinctUntilChanged()
    }
    val current by stateFlow.collectAsStateWithLifecycle(initialValue = null)
    return current
}

private fun createAppSwipeActionSpec(
    actionType: SwipeActionUiModel,
    onAction: () -> Unit,
    backgroundColor: Color,
    iconTint: Color,
) = SwipeActionSpec(
    icon = when (actionType) {
        SwipeActionUiModel.DELETE -> Icons.Default.Delete
        SwipeActionUiModel.DETAIL -> Icons.Default.Info
        SwipeActionUiModel.COPY_PASSWORD -> Icons.Default.ContentCopy
        SwipeActionUiModel.COPY_USERNAME -> Icons.Default.Person
    },
    backgroundColor = backgroundColor,
    iconTint = iconTint,
    onAction = onAction,
)
