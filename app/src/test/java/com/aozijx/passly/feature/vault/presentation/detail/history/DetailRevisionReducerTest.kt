package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DetailRevisionReducerTest {
    @Test
    fun backToListCannotCancelAnActiveRestore() {
        val state = DetailRevisionState.Open(
            entryId = EntryId("entry"),
            revisions = DetailRevisionListState.Ready(listOf(metadata())),
            page = DetailRevisionPage.Comparison(
                revisionId = "revision",
                phase = DetailRevisionComparisonPhase.Ready(
                    differences = emptyList(),
                    restore = DetailRevisionRestorePhase.Restoring,
                ),
            ),
        )

        assertEquals(state, DetailRevisionReducer.reduce(state, DetailRevisionMutation.BackToList))
        assertEquals(state, DetailRevisionReducer.reduce(state, DetailRevisionMutation.Closed))
    }

    @Test
    fun `cancelled authorization releases a claimed restore so it can be retried`() {
        val idle = DetailRevisionState.Open(EntryId("entry"), page = DetailRevisionPage.Comparison(
            "revision", DetailRevisionComparisonPhase.Ready(emptyList()),
        ))
        val confirming = DetailRevisionReducer.reduce(idle, DetailRevisionMutation.RestoreRequested)
        val restoring = DetailRevisionReducer.reduce(confirming, DetailRevisionMutation.RestoreStarted)
        val cancelled = DetailRevisionReducer.reduce(restoring, DetailRevisionMutation.RestoreAborted)
        assertEquals(idle, cancelled)
        val retry = DetailRevisionReducer.reduce(cancelled, DetailRevisionMutation.RestoreRequested)
        assertEquals(confirming, retry)
        assertEquals(restoring, DetailRevisionReducer.reduce(retry, DetailRevisionMutation.RestoreStarted))
    }

    @Test
    fun `restore must be confirmed and cannot be restarted or cancelled by stale dialog actions`() {
        val idle = DetailRevisionState.Open(EntryId("entry"), page = DetailRevisionPage.Comparison(
            "revision", DetailRevisionComparisonPhase.Ready(emptyList()),
        ))
        assertEquals(idle, DetailRevisionReducer.reduce(idle, DetailRevisionMutation.RestoreStarted))
        val confirming = DetailRevisionReducer.reduce(idle, DetailRevisionMutation.RestoreRequested)
        val restoring = DetailRevisionReducer.reduce(confirming, DetailRevisionMutation.RestoreStarted)
        assertEquals(DetailRevisionRestorePhase.Restoring,
            (((restoring as DetailRevisionState.Open).page as DetailRevisionPage.Comparison).phase as DetailRevisionComparisonPhase.Ready).restore)
        assertEquals(restoring, DetailRevisionReducer.reduce(restoring, DetailRevisionMutation.RestoreStarted))
        assertEquals(restoring, DetailRevisionReducer.reduce(restoring, DetailRevisionMutation.RestoreRequested))
        assertEquals(restoring, DetailRevisionReducer.reduce(restoring, DetailRevisionMutation.RestoreCancelled))
    }

    @Test
    fun `open select back and close form one sheet state machine`() {
        val opened = DetailRevisionReducer.reduce(
            DetailRevisionState.Closed,
            DetailRevisionMutation.Opened(EntryId("entry")),
        )
        val listed = DetailRevisionReducer.reduce(opened, DetailRevisionMutation.MetadataChanged("entry", listOf(metadata())))
        val selected = DetailRevisionReducer.reduce(listed, DetailRevisionMutation.ComparisonStarted("entry", "revision"))
        val backed = DetailRevisionReducer.reduce(selected, DetailRevisionMutation.BackToList)
        val closed = DetailRevisionReducer.reduce(backed, DetailRevisionMutation.Closed)

        require(opened is DetailRevisionState.Open)
        require(selected is DetailRevisionState.Open)
        assertEquals(
            DetailRevisionPage.Comparison("revision"),
            selected.page,
        )
        require(backed is DetailRevisionState.Open)
        assertSame(DetailRevisionPage.List, backed.page)
        assertSame(DetailRevisionState.Closed, closed)
    }

    @Test
    fun `stale async result for another entry is ignored`() {
        val state = DetailRevisionReducer.reduce(
            DetailRevisionState.Closed,
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
