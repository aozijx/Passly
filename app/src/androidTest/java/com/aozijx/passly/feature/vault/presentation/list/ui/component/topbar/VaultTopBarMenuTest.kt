package com.aozijx.passly.feature.vault.presentation.list.ui.component.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.feature.vault.presentation.list.VaultCategoriesState
import com.aozijx.passly.feature.vault.presentation.list.VaultRouteRequest
import com.aozijx.passly.feature.vault.presentation.list.VaultUiAction
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortOptionUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultSortUiModel
import com.aozijx.passly.feature.vault.presentation.list.ui.search.VaultSearchState
import com.aozijx.passly.feature.vault.presentation.list.ui.search.VaultSearchStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class VaultTopBarMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun moreMenuKeepsPopupsMeasurableAndDispatchesRoutesAndSortActions() {
        val requests = mutableListOf<VaultRouteRequest>()
        val actions = mutableListOf<VaultUiAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = actions::add,
                    onRouteRequest = requests::add,
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isPopup()).assertNonZero()
        composeRule
            .onNodeWithText(context.getString(R.string.vault_menu_scan_qr))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithText(context.getString(R.string.vault_menu_scan_qr))
            .assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue(requests == listOf(VaultRouteRequest.OpenScanner))
        }

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNode(isPopup()).assertNonZero()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }

        composeRule.waitForIdle()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertTrue(
                requests ==
                    listOf(
                        VaultRouteRequest.OpenScanner,
                        VaultRouteRequest.OpenSettings,
                    ),
            )
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isPopup()).assertNonZero()
        composeRule
            .onNodeWithText(context.getString(R.string.sort_last_used))
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    VaultUiAction.SortOptionSelected(
                        EntrySort(
                            field = EntrySortField.LAST_USED_AT,
                            direction = SortDirection.DESC,
                        ),
                    ),
                ),
                actions,
            )
        }
    }

    private fun SemanticsNodeInteraction.assertNonZero() {
        val bounds = getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left > 0.dp)
        assertTrue(bounds.bottom - bounds.top > 0.dp)
    }

    private fun topBarState() =
        VaultTopBarUiState(
            query = "",
            menu =
                VaultMenuUiState(
                    showTotpCode = false,
                    categorySearchQuery = "",
                    matchingCategories = emptyList(),
                    categoriesState = VaultCategoriesState.ReadyEmpty,
                    selectedCategory = null,
                    selectedSort =
                        VaultSortUiModel.presets.first {
                            it.option == VaultSortOptionUiModel.CREATED_AT
                        },
                ),
            collapseOnScroll = false,
            collapseQuickFilterOnScroll = false,
            hideSystemBars = false,
        )

    private fun searchStateHolder() =
        VaultSearchStateHolder(
            initialState = VaultSearchState.initial(false, ""),
            initialSearchActive = false,
            currentQuery = { "" },
            currentSearchActive = { false },
            onQueryChange = {},
            onSearchActiveChange = {},
            onExpandBars = {},
        )
}
