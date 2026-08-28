package com.aozijx.passly.presentation.ui.database.recovery.model

internal data class DatabaseRecoverySheetState(
    val packages: List<DatabaseRecoveryPackageItem> = emptyList(),
    val isLoading: Boolean = true,
    val activePackageId: String? = null,
    val scan: DatabaseRecoveryScanItem? = null,
    val selectedTypeIds: Set<String> = emptySet(),
    val report: DatabaseRecoveryReportItem? = null,
    val error: String? = null,
    val isClearingDatabase: Boolean = false,
    val databaseCleared: Boolean = false,
) {
    val isBusy: Boolean get() = activePackageId != null
}

internal data class DatabaseRecoveryPackageItem(
    val id: String,
    val createdAtEpochMs: Long,
    val sizeBytes: Long,
    val status: DatabaseRecoveryPackageStatus,
)

internal enum class DatabaseRecoveryPackageStatus {
    PENDING_SCAN,
    RECOVERABLE,
    PARTIALLY_RECOVERABLE,
    RESTORED,
    UNREADABLE,
}

internal data class DatabaseRecoveryScanItem(
    val packageId: String,
    val recoverableTypes: List<DatabaseRecoveryTypeItem>,
    val conflictingEntries: Int,
    val damagedEntries: Int,
    val recoverableAttachments: Int,
) {
    val recoverableEntries: Int get() = recoverableTypes.sumOf { it.count }
}

internal data class DatabaseRecoveryTypeItem(
    val id: String,
    val label: String,
    val count: Int,
)

internal data class DatabaseRecoveryReportItem(
    val restoredEntries: Int,
    val restoredAttachments: Int,
    val restoredRevisions: Int,
    val skippedConflicts: Int,
)

internal interface DatabaseRecoveryEventHandler {
    fun onDismiss()
    fun onClearResult()
    fun onScan(packageId: String)
    fun onRestore(packageId: String)
    fun onToggleType(typeId: String)
    fun onDelete(packageId: String)
    fun onClearDatabase()
}
