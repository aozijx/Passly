package com.aozijx.passly.feature.detail.internal.presentation

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.detail.contract.DetailUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailReducerTest {
    @Test
    fun `title editing transitions are derived only from state and mutation`() {
        val entry = entry(title = "Original")
        val started = DetailReducer.reduce(
            DetailUiState(entry = entry),
            DetailMutation.TitleEditingStarted,
        )
        val changed = DetailReducer.reduce(
            started,
            DetailMutation.EditedTitleChanged("Draft"),
        )
        val cancelled = DetailReducer.reduce(changed, DetailMutation.TitleEditingCancelled)

        assertTrue(started.isEditingTitle)
        assertEquals("Original", started.editedTitle)
        assertEquals("Draft", changed.editedTitle)
        assertFalse(cancelled.isEditingTitle)
        assertEquals("Original", cancelled.editedTitle)
    }

    @Test
    fun `revealed field mutation adds and wipes individual values`() {
        val cvv = OwnedChars.fromString("123")
        try {
            val revealed = DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.RevealedFieldChanged("cvv", cvv),
            )
            val hidden = DetailReducer.reduce(
                revealed,
                DetailMutation.RevealedFieldChanged("cvv", null),
            )

            assertEquals("123", String(revealed.revealed("cvv")!!.toCharArray()))
            assertNull(hidden.revealed("cvv"))
        } finally {
            cvv.wipe()
        }
    }

    @Test
    fun `entry presentation preserves orthogonal screen state`() {
        val password = OwnedChars.fromString("secret")
        try {
            val state = DetailUiState(
                isAccessHistoryEnabled = true,
                revealedFields = mapOf("password" to password),
            )
            val presented = DetailReducer.reduce(
                state,
                DetailMutation.EntryPresented(
                    entry = entry("Updated"),
                    entryType = EntryType.LOGIN,
                    strategySummary = "ready",
                    validationError = null,
                    strategyReady = true,
                    isEditingTitle = false,
                    editedTitle = "Updated",
                ),
            )

            assertTrue(presented.isAccessHistoryEnabled)
            assertEquals("secret", String(presented.revealed("password")!!.toCharArray()))
            assertEquals("Updated", presented.entry?.title)
        } finally {
            password.wipe()
        }
    }

    @Test
    fun `state cleared removes entry and sensitive presentation state`() {
        val password = OwnedChars.fromString("secret")
        try {
            val cleared = DetailReducer.reduce(
                DetailUiState(
                    entry = entry("Secret"),
                    revealedFields = mapOf("password" to password),
                    isFaviconDownloading = true,
                ),
                DetailMutation.StateCleared,
            )

            assertEquals(DetailUiState(), cleared)
        } finally {
            password.wipe()
        }
    }

    private fun entry(title: String) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry-1"),
            type = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = title),
        secret = EntrySecret(credential = LoginCredential()),
    )
}
