package com.aozijx.passly.feature.vault.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.components.cardstyle.CardStyleRegistry
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.VaultTab
import com.aozijx.passly.ui.components.widgets.SwipeDirection
import com.aozijx.passly.ui.components.widgets.SwipeToAction
import com.aozijx.passly.ui.components.widgets.createSwipeAction
import kotlin.collections.getOrNull

@Composable
fun VaultPagerContent(
    pagerState: PagerState,
    uiState: VaultUiState,
    perTypeStyleMap: Map<Int, VaultCardStyle>,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    isSwipeEnabled: Boolean,
    onSwipeTriggered: (SwipeActionType, VaultEntry) -> Unit,
    vaultViewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = uiState.visibleTabs.size.coerceAtLeast(1)
    ) { pageIndex ->
        val currentTab = uiState.visibleTabs.getOrNull(pageIndex) ?: VaultTab.ALL
        val displayItems = uiState.vaultItemsByTab[currentTab] ?: emptyList()

        if (displayItems.isEmpty()) {
            if (!uiState.isVaultItemsLoading) EmptyVaultPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = displayItems, key = { it.id }) { item ->
                    VaultListItemRow(
                        item = item,
                        perTypeStyleMap = perTypeStyleMap,
                        swipeLeftAction = swipeLeftAction,
                        swipeRightAction = swipeRightAction,
                        isSwipeEnabled = isSwipeEnabled,
                        onSwipeTriggered = onSwipeTriggered,
                        vaultViewModel = vaultViewModel
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun VaultListItemRow(
    item: VaultEntry,
    perTypeStyleMap: Map<Int, VaultCardStyle>,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    isSwipeEnabled: Boolean,
    onSwipeTriggered: (SwipeActionType, VaultEntry) -> Unit,
    vaultViewModel: VaultViewModel
) {
    val cardStyle = remember(item.entryType, perTypeStyleMap) {
        perTypeStyleMap[item.entryType.ordinal]?.takeIf { it != VaultCardStyle.DEFAULT }
            ?: VaultCardStyle.DEFAULT
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
            style = cardStyle, entry = item, viewModel = vaultViewModel
        )
    }
}