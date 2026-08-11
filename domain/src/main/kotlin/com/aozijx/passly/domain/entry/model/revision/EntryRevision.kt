package com.aozijx.passly.domain.entry.model.revision

import com.aozijx.passly.domain.entry.model.EntryAggregate

data class EntryRevision(
    val revisionId: String,
    val entryId: String,
    val version: Int,
    val createdAt: Long,
    val changeType: RevisionType,
    val entry: EntryAggregate
)

enum class RevisionType(val value: String) {
    VALUE_CHANGED("value_changed"),
    VERSION_RESTORED("version_restored");

    companion object {
        fun fromValue(value: String): RevisionType =
            entries.find { it.value == value } ?: VALUE_CHANGED
    }
}
