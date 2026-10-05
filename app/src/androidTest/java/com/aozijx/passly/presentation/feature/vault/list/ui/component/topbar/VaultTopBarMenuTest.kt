package com.aozijx.passly.presentation.feature.vault.list.ui.component.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun topBarState() = VaultTopBarUiState(
        query = "",
        showTotpCode = false,
        selectedCategory = null,
        selectedSort = VaultSortUiModel.DEFAULT,
        availableCategories = emptyList(),
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
