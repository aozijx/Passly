package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRevisionHistoryPolicyTest {
    @Test
    fun `summaries report fields changed from the adjacent older revision`() {
        val oldest = revision("oldest", 1, "Old title", username = "old user")
        val middle = revision("middle", 2, "Middle title", username = "old user")
        val current = revision("current", 3, "Middle title", username = "current user")

        val summaries =
            EntryRevisionHistoryPolicy.summarize(
                metadata = listOf(oldest.metadata, current.metadata, middle.metadata),
                snapshots = listOf(oldest, middle, current).associateBy { it.metadata.id },
            )

        assertEquals(listOf(3, 2, 1), summaries.map { it.metadata.version.value })
        assertEquals(setOf(RevisionFieldId.Username), summaries[0].changedFields)
        assertEquals(setOf(RevisionFieldId.Title), summaries[1].changedFields)
        assertEquals(emptySet<RevisionFieldId>(), summaries[2].changedFields)
    }

    @Test
    fun `each revision reports only changes from the adjacent older snapshot`() {
        val unchanged = revision("unchanged", 1, "Current title")
        val changed = revision("changed", 2, "Changed title")
        val current = revision("current", 3, "Current title")

        val summaries =
            EntryRevisionHistoryPolicy.summarize(
                metadata = listOf(current.metadata, unchanged.metadata, changed.metadata),
                snapshots = listOf(unchanged, changed, current).associateBy { it.metadata.id },
            )

        assertEquals(listOf("current", "changed", "unchanged"), summaries.map { it.metadata.id.value })
        assertEquals(setOf(RevisionFieldId.Title), summaries[0].changedFields)
        assertEquals(setOf(RevisionFieldId.Title), summaries[1].changedFields)
        assertEquals(emptySet<RevisionFieldId>(), summaries[2].changedFields)
    }

    @Test
    fun `revisions remain available when a snapshot cannot be loaded`() {
        val historical = revision("historical", 1, "Old title")
        val current = revision("current", 2, "Current title")

        val summaries =
            EntryRevisionHistoryPolicy.summarize(
                metadata = listOf(current.metadata, historical.metadata),
                snapshots = mapOf(current.metadata.id to current),
            )

        assertEquals(listOf("current", "historical"), summaries.map { it.metadata.id.value })
        assertEquals(emptySet<RevisionFieldId>(), summaries.first().changedFields)
        assertEquals(emptySet<RevisionFieldId>(), summaries.last().changedFields)
    }

    private fun revision(
        id: String,
        version: Int,
        title: String,
        username: String = "",
    ) = RedactedEntryRevision(
        metadata =
            EntryRevisionMetadata(
                id = EntryRevisionId(id),
                entryId = ENTRY_ID,
                version = EntryVersion(version),
                createdAtMs = version.toLong(),
                change = RevisionChange.VALUE_CHANGED,
            ),
        type = EntryType.LOGIN,
        profile = EntryProfile(title = title, username = username),
        secret = EntrySecret(credential = LoginCredential()),
    )

    private companion object {
        val ENTRY_ID = EntryId("entry")
    }
}
