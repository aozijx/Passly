package com.aozijx.passly.presentation.feature.vault.list.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSearchStateHolderTest {
    @Test
    fun `clearing results clears query exits search and expands bars`() {
        var query = "mail"
        var searchActive = true
        var barsExpanded = false
        val holder = VaultSearchStateHolder(
            initialState = VaultSearchState.initial(searchActive, query),
            initialSearchActive = searchActive,
            currentQuery = { query },
            currentSearchActive = { searchActive },
            onQueryChange = { query = it },
            onSearchActiveChange = { searchActive = it },
            onExpandBars = { barsExpanded = true },
        )

        holder.clear()

        assertEquals("", query)
        assertFalse(searchActive)
        assertTrue(barsExpanded)
        assertEquals(VaultSearchPhase.BROWSING, holder.uiState.phase)
    }

    @Test
    fun `losing focus after empty edit deactivates search`() {
        var searchActive = false
        val holder = VaultSearchStateHolder(
            initialState = VaultSearchState.initial(searchActive, ""),
            initialSearchActive = searchActive,
            currentQuery = { "" },
            currentSearchActive = { searchActive },
            onQueryChange = {},
            onSearchActiveChange = { searchActive = it },
            onExpandBars = {},
        )

        holder.request()
        holder.focusChanged(true)
        holder.focusChanged(false)

        assertFalse(searchActive)
        assertEquals(VaultSearchPhase.BROWSING, holder.uiState.phase)
    }
}
