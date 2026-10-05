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
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultSortUiModel
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
    fun moreButtonNavigatesSubmenuAndInvokesSettingsNavigation() {
        var settingsClicked = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onSettingsClick = { settingsClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.sort_last_used))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertTrue(settingsClicked)
        }
    }

    @Test
    fun submenuTransitionKeepsOutgoingAndIncomingPagesDuringMotion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
            composeRule.mainClock.advanceTimeBy(100L)

            composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).assertExists()
            composeRule.onNodeWithText(context.getString(R.string.sort_last_used)).assertExists()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun submenuTransitionAnimatesPopupWidthBetweenSettledPages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                VaultTopBar(
                    uiState = topBarState(),
                    searchStateHolder = searchStateHolder(),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onAction = {},
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.waitForIdle()
        val mainBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
        val mainWidth = mainBounds.right - mainBounds.left

        composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
        composeRule.waitForIdle()
        val sortBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
        val sortWidth = sortBounds.right - sortBounds.left

        assertTrue("Main menu must keep its natural width", mainWidth < 280.dp)
        assertTrue("Sort menu must keep its natural width", sortWidth < 280.dp)

        composeRule.onNodeWithText(context.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithText(context.getString(R.string.vault_menu_sort)).performClick()
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(100L)
            val animatedBounds = composeRule.onNode(isPopup()).getUnclippedBoundsInRoot()
            val animatedWidth = animatedBounds.right - animatedBounds.left
            val widthRange = minOf(mainWidth, sortWidth)..maxOf(mainWidth, sortWidth)

            assertTrue("The settled menu pages must have different widths", mainWidth != sortWidth)
            assertTrue(
                "Popup width $animatedWidth must be between $mainWidth and $sortWidth",
                animatedWidth > widthRange.start && animatedWidth < widthRange.endInclusive,
            )
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
            selectedSort = VaultSortUiModel.DEFAULT,
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
