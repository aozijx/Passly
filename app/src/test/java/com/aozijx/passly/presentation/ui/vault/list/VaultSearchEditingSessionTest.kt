package com.aozijx.passly.presentation.ui.vault.list

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSearchEditingSessionTest {
    @Test
    fun `leaving the screen clears editing focus before a later app entry`() {
        val session = VaultSearchEditingSession()
        session.updateEditing(true)

        assertTrue(session.isEditing)

        session.onScreenPaused()

        assertFalse(session.isEditing)
    }
}
