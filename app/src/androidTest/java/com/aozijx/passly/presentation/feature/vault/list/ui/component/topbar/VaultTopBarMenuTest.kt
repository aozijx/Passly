package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
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
import com.aozijx.passly.presentation.feature.vault.list.VaultRouteRequest
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortOptionUiModel
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchState
import com.aozijx.passly.presentation.feature.vault.list.ui.search.VaultSearchStateHolder
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class VaultTopBarMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun moreMenuDispatchesScannerAndSettingsRequestsAfterDismissing() {
        val requests = mutableListOf<VaultRouteRequest>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onRouteRequest = requests::add,
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.vault_menu_scan_qr))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.vault_menu_scan_qr))
            .assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue(requests == listOf(VaultRouteRequest.OpenScanner))
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertTrue(
                requests == listOf(
                    VaultRouteRequest.OpenScanner,
                    VaultRouteRequest.OpenSettings,
                ),
            )
        }
    }

    @Test
    fun menuAndSubmenusKeepNonZeroPopupBounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onRouteRequest = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        val mainBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
        assertTrue(mainBounds.right - mainBounds.left > 0.dp)
        assertTrue(mainBounds.bottom - mainBounds.top > 0.dp)

        composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
        composeRule.waitForIdle()
        val sortBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
        assertTrue(sortBounds.right - sortBounds.left > 0.dp)
        assertTrue(sortBounds.bottom - sortBounds.top > 0.dp)
        composeRule.onNodeWithText(context.getString(R.string.sort_last_used)).assertIsDisplayed()
    }

    @Test
    fun reopenedMenuIsMeasuredBeforeAnimationClockAdvances() {
        val requests = mutableListOf<VaultRouteRequest>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onRouteRequest = requests::add,
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.vault_menu_scan_qr)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
            composeRule.mainClock.advanceTimeByFrame()
            val reopenedBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
            assertTrue(reopenedBounds.right - reopenedBounds.left > 0.dp)
            assertTrue(reopenedBounds.bottom - reopenedBounds.top > 0.dp)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun topBarState() = VaultTopBarUiState(
        query = "",
        menu = VaultMenuUiState(
            showTotpCode = false,
            availableCategories = emptyList(),
            selectedCategory = null,
            selectedSort = VaultSortUiModel.presets.first {
                it.option == VaultSortOptionUiModel.CREATED_AT
            },
        ),
        collapseOnScroll = false,
        collapseQuickFilterOnScroll = false,
        hideSystemBars = false,
    )

    private fun searchStateHolder() = VaultSearchStateHolder(
        initialState = VaultSearchState.initial(false, ""),
        initialSearchActive = false,
        currentQuery = { "" },
        currentSearchActive = { false },
        onQueryChange = {},
        onSearchActiveChange = {},
        onExpandBars = {},
    )
}
