package com.aozijx.passly.feature.vault.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.core.ui.components.widgets.SwipeActionContainer
import com.aozijx.passly.core.ui.components.widgets.createSwipeActionSpec
import com.aozijx.passly.domain.entry.model.OtpUiState
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.VaultQuickFilter
import com.aozijx.passly.feature.vault.components.cardstyle.CardStyleRegistry
import com.aozijx.passly.feature.vault.contract.VaultUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun VaultPagerContent(
    pagerState: PagerState,
    uiState: VaultUiState,
    entryCardPresentations: List<EntryCardPresentation>,
    hierarchyDisplayMode: EntryHierarchyDisplayMode,
    totpStates: StateFlow<Map<String, OtpUiState>>,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    isSwipeEnabled: Boolean,
    onSwipeTriggered: (SwipeActionType, EntryListItem) -> Unit,
    onItemClick: (EntryListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val adaptiveLayout = LocalPasslyAdaptiveLayout.current
    val motionScheme = MaterialTheme.motionScheme
    var playInitialEntryAnimation by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uiState.isVaultItemsLoading, uiState.vaultItemsByQuickFilter) {
        if (!uiState.isVaultItemsLoading &&
            uiState.vaultItemsByQuickFilter.values.any { it.isNotEmpty() }
        ) {
            playInitialEntryAnimation = false
        }
    }

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = 1.coerceAtMost(
            (uiState.visibleQuickFilters.size - 1).coerceAtLeast(0)
        ),
        key = { pageIndex ->
            uiState.visibleQuickFilters.getOrNull(pageIndex)?.settingsKey ?: "vault-empty"
        }
    ) { pageIndex ->
        val currentQuickFilter =
            uiState.visibleQuickFilters.getOrNull(pageIndex) ?: VaultQuickFilter.ALL
        val displayItems = arrangeEntryHierarchy(
            entries = uiState.vaultItemsByQuickFilter[currentQuickFilter].orEmpty(),
            mode = hierarchyDisplayMode
        )

        if (displayItems.isEmpty()) {
            if (!uiState.isVaultItemsLoading) EmptyVaultPlaceholder()
        } else {
            LazyVerticalGrid(
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
                    items = displayItems,
                    key = EntryListItem::id,
                    contentType = EntryListItem::entryType
                ) { item ->
                    EntryListItemRow(
                        item = item,
                        entryCardPresentations = entryCardPresentations,
                        swipeLeftAction = swipeLeftAction,
                        swipeRightAction = swipeRightAction,
                        isSwipeEnabled = isSwipeEnabled,
                        onSwipeTriggered = onSwipeTriggered,
                        onItemClick = { onItemClick(item) },
                        totpStates = totpStates,
                        showTotpCode = uiState.showTOTPCode,
                        animateInitialAppearance = playInitialEntryAnimation,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = motionScheme.defaultSpatialSpec<IntOffset>(),
                                fadeOutSpec = motionScheme.fastEffectsSpec()
                            )
                            .fillMaxWidth()
                    )
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
private fun EntryListItemRow(
    item: EntryListItem,
    entryCardPresentations: List<EntryCardPresentation>,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    isSwipeEnabled: Boolean,
    onSwipeTriggered: (SwipeActionType, EntryListItem) -> Unit,
    onItemClick: () -> Unit,
    totpStates: StateFlow<Map<String, OtpUiState>>,
    showTotpCode: Boolean,
    animateInitialAppearance: Boolean,
    modifier: Modifier = Modifier
) {
    val totpState = if (item.hasOtp) {
        val itemTotpState = remember(item.id, totpStates) {
            totpStates
                .map { states -> states[item.id] }
                .distinctUntilChanged()
        }
        val current by itemTotpState.collectAsStateWithLifecycle(initialValue = null)
        current
    } else {
        null
    }
    val cardStyle = remember(item.entryType, item.capabilityFlags, entryCardPresentations) {
        CardStyleRegistry.resolveStyle(item, entryCardPresentations)
    }
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val visibleState = remember(item.id) {
        MutableTransitionState(!animateInitialAppearance).apply { targetState = true }
    }
    val leftAction =
        remember(item.id, swipeLeftAction, onSwipeTriggered, colorScheme) {
            createSwipeActionSpec(
                actionType = swipeLeftAction,
                onAction = { onSwipeTriggered(swipeLeftAction, item) },
                backgroundColor = if (swipeLeftAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.primary,
                iconTint = Color.White
            )
        }
    val rightAction =
        remember(item.id, swipeRightAction, onSwipeTriggered, colorScheme) {
            createSwipeActionSpec(
                actionType = swipeRightAction,
                onAction = { onSwipeTriggered(swipeRightAction, item) },
                backgroundColor = if (swipeRightAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.secondary,
                iconTint = Color.White
            )
        }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                slideInVertically(
                    animationSpec = motionScheme.defaultSpatialSpec<IntOffset>(),
                    initialOffsetY = { height -> height / 4 }
                )
    ) {
        SwipeActionContainer(
            leftAction = leftAction,
            rightAction = rightAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = isSwipeEnabled,
        ) {
            CardStyleRegistry.RenderVaultItem(
                style = cardStyle,
                entry = item,
                totpState = totpState,
                showTotpCode = showTotpCode,
                onClick = onItemClick
            )
        }
    }
}
