package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.database.recovery.RecoverableDatabasePackage
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseReport
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseScan
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryPackageItem
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryPackageStatus
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryReportItem
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryScanItem
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoverySheetState
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryTypeItem

data class DatabaseRecoveryUiState(
    val recoveryPackages: List<RecoverableDatabasePackage> = emptyList(),
    val isRecoveryLoading: Boolean = true,
    val activeRecoveryPackageId: String? = null,
    val recoveryScan: RecoverableDatabaseScan? = null,
    val selectedRecoveryTypes: Set<EntryType> = emptySet(),
    val recoveryReport: RecoverableDatabaseReport? = null,
    val recoveryError: String? = null,
) {
    val isRecoveryBusy: Boolean get() = activeRecoveryPackageId != null
}

internal fun DatabaseRecoveryUiState.toSheetState() = DatabaseRecoverySheetState(
    packages = recoveryPackages.map { recoveryPackage ->
        DatabaseRecoveryPackageItem(
            id = recoveryPackage.id,
            createdAtEpochMs = recoveryPackage.createdAtMillis,
            sizeBytes = recoveryPackage.sizeBytes,
            status = DatabaseRecoveryPackageStatus.valueOf(recoveryPackage.status.name),
        )
    },
    isLoading = isRecoveryLoading,
    activePackageId = activeRecoveryPackageId,
    scan = recoveryScan?.let { scan ->
        DatabaseRecoveryScanItem(
            packageId = scan.packageId,
            recoverableTypes = scan.recoverableByType.map { (type, count) ->
                DatabaseRecoveryTypeItem(
                    id = type.name,
                    label = type.name.replace('_', ' '),
                    count = count,
                )
            },
            conflictingEntries = scan.conflictingEntries,
            damagedEntries = scan.damagedEntries,
            recoverableAttachments = scan.recoverableAttachments,
        )
    },
    selectedTypeIds = selectedRecoveryTypes.mapTo(linkedSetOf()) { it.name },
    report = recoveryReport?.let { report ->
        DatabaseRecoveryReportItem(
            restoredEntries = report.restoredEntries,
            restoredAttachments = report.restoredAttachments,
            restoredRevisions = report.restoredRevisions,
            skippedConflicts = report.skippedConflicts,
        )
    },
    error = recoveryError,
)

sealed interface DatabaseRecoveryUiAction {
    data object RefreshRecoveryPackages : DatabaseRecoveryUiAction
    data class ScanRecoveryPackage(val packageId: String) : DatabaseRecoveryUiAction
    data class ToggleRecoveryType(val entryType: EntryType) : DatabaseRecoveryUiAction
    data class RestoreRecoveryPackage(val packageId: String) : DatabaseRecoveryUiAction
    data class DeleteRecoveryPackage(val packageId: String) : DatabaseRecoveryUiAction
    data object ClearRecoveryResult : DatabaseRecoveryUiAction
}

internal sealed interface DatabaseRecoveryMutation {
    data class RecoveryPackagesLoaded(
        val packages: List<RecoverableDatabasePackage>,
    ) : DatabaseRecoveryMutation
    data class RecoveryOperationStarted(val packageId: String) : DatabaseRecoveryMutation
    data class RecoveryScanCompleted(val scan: RecoverableDatabaseScan) : DatabaseRecoveryMutation
    data class RecoveryTypeToggled(val entryType: EntryType) : DatabaseRecoveryMutation
    data class RecoveryRestoreCompleted(val report: RecoverableDatabaseReport) : DatabaseRecoveryMutation
    data class RecoveryOperationFailed(val message: String) : DatabaseRecoveryMutation
    data object RecoveryResultCleared : DatabaseRecoveryMutation
}
