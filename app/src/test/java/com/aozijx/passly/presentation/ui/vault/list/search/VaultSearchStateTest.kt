package com.aozijx.passly.presentation.ui.vault.list.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSearchStateTest {
    @Test
    fun `pull state follows bounded gesture progress`() {
        val state = VaultSearchState.initial(isSearchActive = false, query = "")
            .onPullProgressChanged(0.45f)

        assertEquals(VaultSearchPhase.PULLING, state.phase)
        assertEquals(0.45f, state.layoutProgress)
        assertTrue(state.isDirectManipulation)
        assertEquals(1f, state.onPullProgressChanged(1.4f).layoutProgress)
    }

    @Test
    fun `initial focus loss cannot cancel a pending focus request`() {
        var state = VaultSearchState.initial(isSearchActive = false, query = "")
            .startEditing()

        state = state.onFocusChanged(focused = false, query = "")

        assertEquals(VaultSearchPhase.EDITING, state.phase)

        state = state.onFocusChanged(focused = true, query = "")
        state = state.onFocusChanged(focused = false, query = "mail")

        assertEquals(VaultSearchPhase.RESULTS, state.phase)
    }

    @Test
    fun `gesture reset cannot cancel editing after pull threshold`() {
        val state = VaultSearchState.initial(isSearchActive = false, query = "")
            .onPullProgressChanged(0.9f)
            .startEditing()
            .onPullProgressChanged(0f)

        assertEquals(VaultSearchPhase.EDITING, state.phase)
    }

    @Test
    fun `settling and pausing preserve non blank results without editing focus`() {
        val editing = VaultSearchState.initial(isSearchActive = true, query = "mail")
            .startEditing()
            .onFocusChanged(focused = true, query = "mail")

        val submitted = editing.settle(query = "mail")
        val paused = editing.onScreenPaused(query = "mail")

        assertEquals(VaultSearchPhase.RESULTS, submitted.phase)
        assertEquals(VaultSearchPhase.RESULTS, paused.phase)
        assertFalse(paused.isEditing)
    }

    @Test
    fun `external search state resolves browsing and compact results`() {
        val browsing = VaultSearchState.initial(isSearchActive = false, query = "")
        val results = browsing.synchronize(isSearchActive = true, query = "mail")
        val exited = results.synchronize(isSearchActive = false, query = "mail")

        assertEquals(VaultSearchPhase.BROWSING, browsing.phase)
        assertEquals(VaultSearchPhase.RESULTS, results.phase)
        assertEquals(VaultSearchPhase.BROWSING, exited.phase)
    }
}
