package com.aozijx.passly.presentation.feature.vault.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListDisplayUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListContentUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultListNavigationUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultListStateIsolationComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dialogAndSearchUpdatesDoNotRecomposePagingNode() {
        val uiState = mutableStateOf(VaultUiState())
        var pagingCompositions = 0

        composeRule.setContent {
            val mapped = uiState.value.toUiModel(display())
            PagingProbe(mapped.navigation, mapped.content) { pagingCompositions++ }
        }
        composeRule.runOnIdle { assertEquals(1, pagingCompositions) }

        composeRule.runOnIdle { uiState.value = uiState.value.copy(addType = com.aozijx.passly.feature.vault.model.AddType.BANK_CARD) }
        composeRule.runOnIdle { uiState.value = uiState.value.copy(searchQuery = "mail") }
        composeRule.runOnIdle { assertEquals(1, pagingCompositions) }
    }

    @Composable
    private fun PagingProbe(
        navigation: VaultListNavigationUiModel,
        content: VaultListContentUiModel,
        onComposition: () -> Unit,
    ) {
        navigation.filterOptions
        content.showTotpCode
        onComposition()
    }

    private fun display() = VaultListDisplayUiModel(
        cardPresentations = emptyList(),
        swipeLeftAction = SwipeActionUiModel.DELETE,
        swipeRightAction = SwipeActionUiModel.DETAIL,
        isSwipeEnabled = true,
        collapseTopBarOnScroll = false,
        collapseQuickFilterBarOnScroll = false,
        hideSystemBars = false,
    )
}
