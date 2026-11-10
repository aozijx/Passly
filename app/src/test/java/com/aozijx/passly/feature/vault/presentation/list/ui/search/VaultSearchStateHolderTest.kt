package com.aozijx.passly.feature.vault.presentation.list.ui.search

import com.aozijx.passly.feature.vault.presentation.list.VaultSearchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSearchStateHolderTest {
    @Test
    fun `unexpected platform focus does not activate search`() {
        var searchActive = false
        val holder =
            VaultSearchStateHolder(
                initialState = VaultSearchState.initial(VaultSearchInput.Closed),
                initialSearch = VaultSearchInput.Closed,
                currentSearch = { if (searchActive) VaultSearchInput.Open("") else VaultSearchInput.Closed },
                onQueryChange = {},
                onSearchActiveChange = { searchActive = it },
                onExpandBars = {},
            )

        holder.focusChanged(true)

        assertFalse(searchActive)
        assertEquals(VaultSearchState.Browsing, holder.uiState)
    }

    @Test
    fun `clearing results clears query exits search and expands bars`() {
        var query = "mail"
        var searchActive = true
        var barsExpanded = false
        val holder =
            VaultSearchStateHolder(
                initialState = VaultSearchState.initial(VaultSearchInput.Open(query)),
                initialSearch = VaultSearchInput.Open(query),
                currentSearch = { if (searchActive) VaultSearchInput.Open(query) else VaultSearchInput.Closed },
                onQueryChange = { query = it },
                onSearchActiveChange = { searchActive = it },
                onExpandBars = { barsExpanded = true },
            )

        holder.clear()

        assertEquals("", query)
        assertFalse(searchActive)
        assertTrue(barsExpanded)
        assertEquals(VaultSearchState.Browsing, holder.uiState)
    }

    @Test
    fun `losing focus after empty edit deactivates search`() {
        var searchActive = false
        val holder =
            VaultSearchStateHolder(
                initialState = VaultSearchState.initial(VaultSearchInput.Closed),
                initialSearch = VaultSearchInput.Closed,
                currentSearch = { if (searchActive) VaultSearchInput.Open("") else VaultSearchInput.Closed },
                onQueryChange = {},
                onSearchActiveChange = { searchActive = it },
                onExpandBars = {},
            )

        holder.request()
        holder.focusChanged(true)
        holder.focusChanged(false)

        assertFalse(searchActive)
        assertEquals(VaultSearchState.Browsing, holder.uiState)
    }
}
