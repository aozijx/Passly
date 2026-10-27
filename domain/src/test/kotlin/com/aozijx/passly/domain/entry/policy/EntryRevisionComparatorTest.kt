package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryRevisionComparatorTest {
    private val comparator = EntryRevisionComparator

    @Test
    fun `wifi passwords cannot enter ordinary comparison payloads`() {
        val historical = revision("old").copy(type = EntryType.WIFI, secret = EntrySecret(credential = WifiCredential("network", "old-wifi-secret")))
        val current = revision("current").copy(type = EntryType.WIFI, secret = EntrySecret(credential = WifiCredential("network", "new-wifi-secret")))
        val differences = comparator.compare(historical, current)
        assertFalse(differences.toString().contains("old-wifi-secret"))
        assertFalse(differences.toString().contains("new-wifi-secret"))
    }

    @Test
    fun `hidden custom field values cannot enter ordinary comparison payloads`() {
        val historical = revision("old").copy(secret = EntrySecret(credential = LoginCredential(), customFields = listOf(
            CustomField("private", "old-hidden-secret", CustomFieldKind.HIDDEN),
        )))
        val current = revision("current").copy(secret = EntrySecret(credential = LoginCredential(), customFields = listOf(
            CustomField("private", "new-hidden-secret", CustomFieldKind.HIDDEN),
        )))
        val differences = comparator.compare(historical, current)
        assertFalse(differences.toString().contains("old-hidden-secret"))
        assertFalse(differences.toString().contains("new-hidden-secret"))
    }

    @Test
    fun `credential comparison identifies readable fields individually`() {
        val historical = revision("old").copy(secret = EntrySecret(credential = LoginCredential(email = "before@example.test")))
        val current = revision("current").copy(secret = EntrySecret(credential = LoginCredential(email = "after@example.test")))
        val difference = comparator.compare(historical, current).single { it.field == RevisionFieldId.CredentialField(FieldKey.EMAIL) }
        assertEquals(RevisionFieldValue.Text("before@example.test"), difference.before)
        assertEquals(RevisionFieldValue.Text("after@example.test"), difference.after)
    }

    @Test
    fun `security answers are protected even though stored in the bundle`() {
        val historical = revision("old").copy(type = EntryType.ID_CARD,
            secret = EntrySecret(credential = IdentityCredential(securityAnswer = "old-private-answer")))
        val current = revision("current").copy(type = EntryType.ID_CARD,
            secret = EntrySecret(credential = IdentityCredential(securityAnswer = "new-private-answer")))
        val differences = comparator.compare(historical, current)
        val answer = differences.single { it.field == RevisionFieldId.ProtectedField(FieldKey.SECURITY_ANSWER) }
        assertEquals(RevisionDifferenceKind.CHANGED, answer.kind)
        assertNull(answer.before)
        assertNull(answer.after)
        assertFalse(differences.toString().contains("private-answer"))
    }

    @Test
    fun `revealing custom field kind does not expose its historical hidden value`() {
        val historical = revision("old").copy(secret = EntrySecret(credential = LoginCredential(), customFields = listOf(
            CustomField("same name", "hidden-history", CustomFieldKind.HIDDEN),
        )))
        val current = revision("current").copy(secret = EntrySecret(credential = LoginCredential(), customFields = listOf(
            CustomField("same name", "public-current", CustomFieldKind.TEXT),
        )))
        val difference = comparator.compare(historical, current).single { it.field is RevisionFieldId.ProtectedCustomField }
        assertEquals(RevisionDifferenceKind.CHANGED, difference.kind)
        assertNull(difference.before)
        assertNull(difference.after)
    }

    @Test
    fun `wifi names preserve whitespace and booleans remain typed`() {
        val historical = revision("old").copy(type = EntryType.WIFI,
            secret = EntrySecret(credential = WifiCredential(" Office ", isHidden = false)))
        val current = revision("current").copy(type = EntryType.WIFI,
            secret = EntrySecret(credential = WifiCredential("Office", isHidden = true)))
        val differences = comparator.compare(historical, current).associateBy { it.field }
        assertEquals(RevisionFieldValue.Text(" Office "), differences.getValue(RevisionFieldId.CredentialField(FieldKey.WIFI_SSID)).before)
        assertEquals(RevisionFieldValue.Flag(true), differences.getValue(RevisionFieldId.CredentialField(FieldKey.WIFI_HIDDEN)).after)
    }

    @Test
    fun `protected differences reject accidentally supplied text values`() {
        assertThrows(IllegalArgumentException::class.java) {
            EntryRevisionDifference(RevisionFieldId.ProtectedField(FieldKey.PASSWORD), RevisionDifferenceKind.CHANGED,
                RevisionFieldValue.Text("must-not-enter"), null)
        }
    }

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
        type = EntryType.LOGIN,
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
