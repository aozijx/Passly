package com.aozijx.passly.data.database.model

import com.aozijx.passly.domain.entry.model.EntryType

enum class DatabaseRecoveryStatus {
    PENDING_SCAN,
    RECOVERABLE,
    PARTIALLY_RECOVERABLE,
    RESTORED,
    UNREADABLE,
}

data class DatabaseRecoveryPackage(
    val id: String,
    val createdAtEpochMs: Long,
    val sizeBytes: Long,
    val status: DatabaseRecoveryStatus,
)

data class DatabaseRecoveryIssue(
    val category: String,
    val anonymousRecordId: String? = null,
    val reasonCode: String,
)

data class DatabaseRecoveryScan(
    val packageId: String,
    val recoverableByType: Map<EntryType, Int>,
    val deletedEntries: Int,
    val conflictingEntries: Int,
    val damagedEntries: Int,
    val recoverableAttachments: Int,
    val damagedResources: Int,
    val issues: List<DatabaseRecoveryIssue>,
) {
    val recoverableEntries: Int get() = recoverableByType.values.sum()
    val isPartial: Boolean get() = damagedEntries > 0 || damagedResources > 0
}

data class DatabaseRecoverySelection(
    val entryTypes: Set<EntryType>,
)

data class DatabaseRecoveryReport(
    val packageId: String,
    val restoredEntries: Int,
    val skippedConflicts: Int,
    val restoredAttachments: Int,
    val skippedResources: Int,
    val restoredRevisions: Int,
    val restoredLinks: Int,
    val issues: List<DatabaseRecoveryIssue>,
)
