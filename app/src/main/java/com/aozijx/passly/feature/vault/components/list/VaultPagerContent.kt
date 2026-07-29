package com.aozijx.passly.feature.vault.components.list

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.ui.adaptive.LocalPasslyAdaptiveLayout
import com.aozijx.passly.core.ui.components.widgets.SwipeDirection
import com.aozijx.passly.core.ui.components.widgets.SwipeToAction
import com.aozijx.passly.core.ui.components.widgets.createSwipeAction
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.feature.vault.components.cardstyle.CardStyleRegistry
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.feature.vault.model.VaultTab
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

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = 1.coerceAtMost(
            (uiState.visibleTabs.size - 1).coerceAtLeast(0)
        ),
        key = { pageIndex ->
            uiState.visibleTabs.getOrNull(pageIndex)?.settingsKey ?: "vault-empty"
        }
    ) { pageIndex ->
        val currentTab = uiState.visibleTabs.getOrNull(pageIndex) ?: VaultTab.ALL
        val displayItems = arrangeEntryHierarchy(
            entries = uiState.vaultItemsByTab[currentTab].orEmpty(),
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
                        showTotpCode = uiState.showTOTPCode
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
    showTotpCode: Boolean
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
    val actions =
        remember(item.id, swipeLeftAction, swipeRightAction, onSwipeTriggered, colorScheme) {
            listOfNotNull(
                createSwipeAction(
                    actionType = swipeLeftAction,
                    direction = SwipeDirection.LEFT,
                    onAction = { onSwipeTriggered(swipeLeftAction, item) },
                    backgroundColor = if (swipeLeftAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.primary,
                    iconTint = Color.White
                ), createSwipeAction(
                    actionType = swipeRightAction,
                    direction = SwipeDirection.RIGHT,
                    onAction = { onSwipeTriggered(swipeRightAction, item) },
                    backgroundColor = if (swipeRightAction == SwipeActionType.DELETE) colorScheme.error else colorScheme.secondary,
                    iconTint = Color.White
                )
            )
        }

    SwipeToAction(
        actions = actions,
        modifier = Modifier.fillMaxWidth(),
        isActive = isSwipeEnabled,
    ) {
        CardStyleRegistry.RenderVaultItem(
            style = cardStyle, entry = item,
            totpState = totpState,
            showTotpCode = showTotpCode,
            onClick = onItemClick
        )
    }
}
