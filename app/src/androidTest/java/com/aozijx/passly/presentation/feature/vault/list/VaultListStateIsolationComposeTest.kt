package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListContentUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListNavigationUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultListStateIsolationComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dialogSearchAndFabUpdatesDoNotRecomposePagingNode() {
        val uiState = mutableStateOf(VaultUiState())
        val fabVisible = mutableStateOf(true)
        var pagingCompositions = 0

        composeRule.setContent {
            val mapped = uiState.value.toUiModel(display(fabVisible.value), false)
            val stable = rememberVaultListScreenUiModel(mapped)
            PagingProbe(stable.navigation, stable.content) { pagingCompositions++ }
        }
        composeRule.runOnIdle { assertEquals(1, pagingCompositions) }

        composeRule.runOnIdle { uiState.value = uiState.value.copy(addType = com.aozijx.passly.feature.vault.model.AddType.BANK_CARD) }
        composeRule.runOnIdle { uiState.value = uiState.value.copy(searchQuery = "mail") }
        composeRule.runOnIdle { fabVisible.value = false }

        composeRule.runOnIdle { assertEquals(1, pagingCompositions) }
    }

    @Composable
    private fun PagingProbe(
        navigation: VaultListNavigationUiModel,
        content: VaultListContentUiModel,
        onComposition: () -> Unit,
    ) {
        navigation.visibleQuickFilters
        content.showTotpCode
        onComposition()
    }

    private fun display(isFabVisible: Boolean) = VaultListDisplayUiModel(
        cardPresentations = emptyList(),
        swipeLeftAction = SwipeActionUiModel.DELETE,
        swipeRightAction = SwipeActionUiModel.DETAIL,
        isSwipeEnabled = true,
        isFabVisible = isFabVisible,
        collapseTopBarOnScroll = false,
        collapseQuickFilterBarOnScroll = false,
        hideSystemBars = false,
    )
}
