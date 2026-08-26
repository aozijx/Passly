package com.aozijx.passly.feature.database.recovery

import com.aozijx.passly.domain.entry.model.EntryType

enum class RecoverableDatabaseStatus {
    PENDING_SCAN,
    RECOVERABLE,
    PARTIALLY_RECOVERABLE,
    RESTORED,
    UNREADABLE,
}

data class RecoverableDatabasePackage(
    val id: String,
    val createdAtMillis: Long,
    val sizeBytes: Long,
    val status: RecoverableDatabaseStatus,
)

data class RecoverableDatabaseIssue(
    val category: String,
    val anonymousRecordId: String? = null,
    val reasonCode: String,
)

data class RecoverableDatabaseScan(
    val packageId: String,
    val recoverableByType: Map<EntryType, Int>,
    val deletedEntries: Int,
    val conflictingEntries: Int,
    val damagedEntries: Int,
    val recoverableAttachments: Int,
    val damagedResources: Int,
    val issues: List<RecoverableDatabaseIssue>,
)

data class RecoverableDatabaseReport(
    val packageId: String,
    val restoredEntries: Int,
    val skippedConflicts: Int,
    val restoredAttachments: Int,
    val skippedResources: Int,
    val restoredRevisions: Int,
    val restoredLinks: Int,
    val issues: List<RecoverableDatabaseIssue>,
)
