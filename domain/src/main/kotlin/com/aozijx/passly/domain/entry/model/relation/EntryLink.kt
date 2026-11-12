package com.aozijx.passly.domain.entry.model.relation

import com.aozijx.passly.domain.entry.model.EntryId

@JvmInline
value class EntryLinkId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Entry link ID cannot be blank" }
    }
}

/** The inferred meaning of a generic link; never persisted on the link itself. */
enum class EntryLinkKind {
    ACCOUNT_MEMBERSHIP,
    OTP_ASSOCIATION,
    RECOVERY_ASSOCIATION,
    RELATED_ENTRIES,
}

/** A generic, undirected association between two independently stored entries. */
class EntryLink private constructor(
    val id: EntryLinkId,
    val firstEntryId: EntryId,
    val secondEntryId: EntryId,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(firstEntryId != secondEntryId) { "An entry cannot link to itself" }
        require(firstEntryId.value < secondEntryId.value) { "Entry link endpoints must be canonical" }
    }

    fun otherEndpoint(entryId: EntryId): EntryId? =
        when (entryId) {
            firstEntryId -> secondEntryId
            secondEntryId -> firstEntryId
            else -> null
        }

    companion object {
        fun create(
            id: EntryLinkId,
            firstEntryId: EntryId,
            secondEntryId: EntryId,
            createdAt: Long,
            updatedAt: Long = createdAt,
        ): EntryLink {
            val (first, second) =
                if (firstEntryId.value < secondEntryId.value) firstEntryId to secondEntryId else secondEntryId to firstEntryId
            return EntryLink(
                id = id,
                firstEntryId = first,
                secondEntryId = second,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }

    override fun equals(other: Any?): Boolean =
        other is EntryLink &&
            id == other.id &&
            firstEntryId == other.firstEntryId &&
            secondEntryId == other.secondEntryId &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt

    override fun hashCode(): Int = arrayOf(id, firstEntryId, secondEntryId, createdAt, updatedAt).contentHashCode()
}
