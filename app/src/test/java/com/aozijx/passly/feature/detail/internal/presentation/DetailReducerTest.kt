package com.aozijx.passly.feature.detail.internal.presentation

import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
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
        val revealed = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.RevealedFieldChanged("cvv", "123"),
        )
        val hidden = DetailReducer.reduce(
            revealed,
            DetailMutation.RevealedFieldChanged("cvv", null),
        )

        assertEquals("123", revealed.revealed("cvv"))
        assertNull(hidden.revealed("cvv"))
    }

    @Test
    fun `entry presentation preserves orthogonal screen state`() {
        val state = DetailUiState(
            isAccessHistoryEnabled = true,
            revealedFields = mapOf("password" to "secret"),
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
        assertEquals("secret", presented.revealed("password"))
        assertEquals("Updated", presented.entry?.title)
    }

    @Test
    fun `state cleared removes entry and sensitive presentation state`() {
        val cleared = DetailReducer.reduce(
            DetailUiState(
                entry = entry("Secret"),
                revealedFields = mapOf("password" to "secret"),
                isFaviconDownloading = true,
            ),
            DetailMutation.StateCleared,
        )

        assertEquals(DetailUiState(), cleared)
    }

    private fun entry(title: String) = EntryAggregate(
        header = EntryHeader(
            id = EntryId("entry-1"),
            entryType = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            createdAt = 1L,
            updatedAt = 1L,
        ),
        summary = EntrySummary(title = title, username = ""),
        secret = EntrySecret(),
    )
}
