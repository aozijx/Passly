package com.aozijx.passly.domain.entry.model.history

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
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

enum class RevisionChange { VALUE_CHANGED, VERSION_RESTORED }
