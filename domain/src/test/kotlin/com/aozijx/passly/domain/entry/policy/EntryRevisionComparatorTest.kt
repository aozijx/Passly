package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryRevisionComparatorTest {
    private val comparator = EntryRevisionComparator

    @Test
    fun `comparison classifies ordinary and structural changes`() {
        val historical = revision(
            id = "old",
            title = "Old",
            notes = null,
            attachments = setOf("a"),
        )
        val current = revision(
            id = "current",
            title = "Current",
            notes = "note",
            attachments = emptySet(),
        )

        val differences = comparator.compare(historical, current).associateBy { it.field }

        assertEquals(RevisionDifferenceKind.CHANGED, differences.getValue(RevisionFieldId.Title).kind)
        assertEquals(RevisionDifferenceKind.ADDED, differences.getValue(RevisionFieldId.Notes).kind)
        assertEquals(RevisionDifferenceKind.REMOVED, differences.getValue(RevisionFieldId.Attachments).kind)
    }

    @Test
    fun `sensitive differences expose keys and kinds but never values`() {
        val historical = revision(id = "old", sensitive = setOf(SensitiveFieldKey.PASSWORD))
        val current = revision(id = "current", sensitive = setOf(SensitiveFieldKey.OTP_SECRET))

        val differences = comparator.compare(historical, current)
            .filter { it.field is RevisionFieldId.Sensitive }

        assertEquals(2, differences.size)
        assertEquals(
            RevisionDifferenceKind.REMOVED,
            differences.single { it.field == RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD) }.kind,
        )
        assertEquals(
            RevisionDifferenceKind.ADDED,
            differences.single { it.field == RevisionFieldId.Sensitive(SensitiveFieldKey.OTP_SECRET) }.kind,
        )
        differences.forEach {
            assertNull(it.before)
            assertNull(it.after)
        }
    }

    @Test
    fun `opaque fingerprints classify changed sensitive values without exposing them`() {
        val historical = revision(
            id = "old",
            sensitive = setOf(SensitiveFieldKey.PASSWORD),
            fingerprints = mapOf(SensitiveFieldKey.PASSWORD to "old-token"),
        )
        val current = revision(
            id = "current",
            sensitive = setOf(SensitiveFieldKey.PASSWORD),
            fingerprints = mapOf(SensitiveFieldKey.PASSWORD to "new-token"),
        )

        val difference = comparator.compare(historical, current)
            .single { it.field == RevisionFieldId.Sensitive(SensitiveFieldKey.PASSWORD) }

        assertEquals(RevisionDifferenceKind.CHANGED, difference.kind)
        assertNull(difference.before)
        assertNull(difference.after)
    }

    private fun revision(
        id: String,
        title: String = "Title",
        notes: String? = null,
        attachments: Set<String> = emptySet(),
        sensitive: Set<SensitiveFieldKey> = emptySet(),
        fingerprints: Map<SensitiveFieldKey, String> = emptyMap(),
    ) = RedactedEntryRevision(
        metadata = EntryRevisionMetadata(
            id = EntryRevisionId(id),
            entryId = EntryId("entry"),
            version = EntryVersion(if (id == "old") 1 else 2),
            createdAtMs = 1L,
            change = RevisionChange.VALUE_CHANGED,
        ),
        profile = EntryProfile(title = title),
        secret = EntrySecret(credential = LoginCredential(), notes = notes),
        attachmentIds = attachments,
        sensitiveFieldKeys = sensitive,
        sensitiveFieldFingerprints = fingerprints,
    )
}
