package com.aozijx.passly.domain.entry.model.history

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

@JvmInline
value class EntryRevisionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Entry revision ID cannot be blank" }
    }
}

data class EntryRevision(
    val id: EntryRevisionId,
    val entryId: EntryId,
    val version: EntryVersion,
    val createdAtMs: Long,
    val change: RevisionChange,
    val snapshot: Entry,
    val links: List<EntryLink> = emptyList(),
    val attachmentIds: Set<String> = emptySet(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
) {
    init {
        require(createdAtMs >= 0L) { "Revision creation time cannot be negative" }
        require(snapshot.id == entryId) { "Revision snapshot must belong to the same entry" }
    }
}

/** Decryption-free data used to render one revision in a history list. */
data class EntryRevisionMetadata(
    val id: EntryRevisionId,
    val entryId: EntryId,
    val version: EntryVersion,
    val createdAtMs: Long,
    val change: RevisionChange,
) {
    init {
        require(createdAtMs >= 0L) { "Revision creation time cannot be negative" }
    }
}

/**
 * A revision snapshot safe for ordinary comparison.
 *
 * [secret] contains only the low-sensitivity structural bundle. Historical field-level values
 * are represented by [sensitiveFieldKeys] and must be revealed through revision authorization.
 */
data class RedactedEntryRevision(
    val metadata: EntryRevisionMetadata,
    val profile: EntryProfile,
    val secret: EntrySecret,
    val links: List<EntryLink> = emptyList(),
    val attachmentIds: Set<String> = emptySet(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    /** Opaque ciphertext fingerprints used only to classify changed versus unchanged values. */
    val sensitiveFieldFingerprints: Map<SensitiveFieldKey, String> = emptyMap(),
) {
    init {
        require(sensitiveFieldKeys.containsAll(sensitiveFieldFingerprints.keys)) {
            "Sensitive fingerprints must belong to present fields"
        }
    }
}

enum class RevisionChange { VALUE_CHANGED, VERSION_RESTORED }
