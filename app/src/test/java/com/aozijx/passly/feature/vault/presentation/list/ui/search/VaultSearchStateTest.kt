package com.aozijx.passly.feature.vault.presentation.list.ui.search

import com.aozijx.passly.feature.vault.presentation.list.VaultSearchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSearchStateTest {
    @Test
    fun `pull state follows bounded gesture progress`() {
        val state =
            VaultSearchState
                .initial(VaultSearchInput.Closed)
                .onPullProgressChanged(0.45f)

        assertEquals(VaultSearchState.Pulling(0.45f), state)
        assertEquals(0.45f, state.layoutProgress)
        assertTrue(state.isDirectManipulation)
        assertEquals(1f, state.onPullProgressChanged(1.4f).layoutProgress)
    }

    @Test
    fun `initial focus loss cannot cancel a pending focus request`() {
        var state: VaultSearchState = VaultSearchState.Editing()

        state = state.onFocusChanged(focused = false, query = "")

        assertEquals(VaultSearchState.Editing(), state)

        state = state.onFocusChanged(focused = true, query = "")
        state = state.onFocusChanged(focused = false, query = "mail")

        assertEquals(VaultSearchState.Results, state)
    }

    @Test
    fun `restored focus cannot start editing without an explicit request`() {
        val state =
            VaultSearchState
                .initial(VaultSearchInput.Closed)
                .onFocusChanged(focused = true, query = "")

        assertEquals(VaultSearchState.Browsing, state)
    }

    @Test
    fun `gesture reset cannot cancel editing after pull threshold`() {
        val state =
            VaultSearchState.Editing().onPullProgressChanged(0f)

        assertEquals(VaultSearchState.Editing(), state)
    }

    @Test
    fun `settling preserves non blank results without editing focus`() {
        val editing =
            VaultSearchState
                .Editing()
                .onFocusChanged(focused = true, query = "mail")

        val submitted = settle(query = "mail")

        assertEquals(VaultSearchState.Results, submitted)
        assertFalse(submitted.isEditing)
    }

    @Test
    fun `external search state resolves browsing and compact results`() {
        val browsing = VaultSearchState.initial(VaultSearchInput.Closed)
        val results = browsing.synchronize(VaultSearchInput.Open("mail"))
        val exited = results.synchronize(VaultSearchInput.Closed)

        assertEquals(VaultSearchState.Browsing, browsing)
        assertEquals(VaultSearchState.Results, results)
        assertEquals(VaultSearchState.Browsing, exited)
    }
}
