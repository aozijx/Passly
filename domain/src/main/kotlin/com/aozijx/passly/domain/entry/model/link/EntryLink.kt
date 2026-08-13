package com.aozijx.passly.domain.entry.model.link

import com.aozijx.passly.domain.entry.model.EntryId

@JvmInline
value class EntryLinkId(val value: String)

/**
 * Explicit relations between independently stored entries.
 *
 * ACCOUNT is the grouping hub. Credentials point at it instead of forming an N x N graph.
 */
enum class EntryRelationType {
    MEMBER_OF_ACCOUNT,
    OTP_FOR,
    RECOVERY_FOR,
    RELATED_TO;

    val isSymmetric: Boolean
        get() = this == RELATED_TO
}

/**
 * A typed edge in the entry graph.
 *
 * Symmetric edges use a canonical endpoint order so the database can enforce uniqueness.
 */
data class EntryLink private constructor(
    val id: EntryLinkId,
    val sourceEntryId: EntryId,
    val targetEntryId: EntryId,
    val relationType: EntryRelationType,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(sourceEntryId != targetEntryId) { "An entry cannot link to itself" }
        require(
            !relationType.isSymmetric || sourceEntryId.value < targetEntryId.value
        ) { "Symmetric entry links must use canonical endpoint order" }
    }

    companion object {
        fun create(
            id: EntryLinkId,
            sourceEntryId: EntryId,
            targetEntryId: EntryId,
            relationType: EntryRelationType,
            createdAt: Long,
            updatedAt: Long = createdAt
        ): EntryLink {
            val (source, target) = if (
                relationType.isSymmetric && sourceEntryId.value > targetEntryId.value
            ) {
                targetEntryId to sourceEntryId
            } else {
                sourceEntryId to targetEntryId
            }
            return EntryLink(
                id = id,
                sourceEntryId = source,
                targetEntryId = target,
                relationType = relationType,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
