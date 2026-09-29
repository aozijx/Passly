package com.aozijx.passly.presentation.feature.vault.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRevisionReducerTest {
    @Test
    fun `open select back and close form one sheet state machine`() {
        val opened = DetailRevisionReducer.reduce(
            DetailRevisionState(),
            DetailRevisionMutation.Opened(EntryId("entry")),
        )
        val listed = DetailRevisionReducer.reduce(opened, DetailRevisionMutation.MetadataChanged("entry", listOf(metadata())))
        val selected = DetailRevisionReducer.reduce(listed, DetailRevisionMutation.ComparisonStarted("entry", "revision"))
        val backed = DetailRevisionReducer.reduce(selected, DetailRevisionMutation.BackToList)
        val closed = DetailRevisionReducer.reduce(backed, DetailRevisionMutation.Closed)

        assertTrue(opened.visible)
        assertEquals(DetailRevisionDestination.COMPARISON, selected.destination)
        assertEquals(DetailRevisionDestination.LIST, backed.destination)
        assertFalse(closed.visible)
        assertEquals(null, closed.entryId)
    }

    @Test
    fun `stale async result for another entry is ignored`() {
        val state = DetailRevisionReducer.reduce(
            DetailRevisionState(),
            DetailRevisionMutation.Opened(EntryId("entry")),
        )

        val result = DetailRevisionReducer.reduce(
            state,
            DetailRevisionMutation.MetadataChanged("other", listOf(metadata())),
        )

        assertEquals(state, result)
    }

    private fun metadata() = EntryRevisionMetadata(
        id = EntryRevisionId("revision"),
        entryId = EntryId("entry"),
        version = EntryVersion(1),
        createdAtMs = 1L,
        change = RevisionChange.VALUE_CHANGED,
    )
}
