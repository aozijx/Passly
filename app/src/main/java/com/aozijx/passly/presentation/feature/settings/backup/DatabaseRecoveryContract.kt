package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.domain.entry.model.EntryType

data class DatabaseRecoveryUiState(
    val recoveryPackages: List<DatabaseRecoveryPackage> = emptyList(),
    val isRecoveryLoading: Boolean = true,
    val activeRecoveryPackageId: String? = null,
    val recoveryScan: DatabaseRecoveryScan? = null,
    val selectedRecoveryTypes: Set<EntryType> = emptySet(),
    val recoveryReport: DatabaseRecoveryReport? = null,
    val recoveryError: String? = null,
) {
    val isRecoveryBusy: Boolean get() = activeRecoveryPackageId != null
}

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
        val packages: List<DatabaseRecoveryPackage>,
    ) : DatabaseRecoveryMutation
    data class RecoveryOperationStarted(val packageId: String) : DatabaseRecoveryMutation
    data class RecoveryScanCompleted(val scan: DatabaseRecoveryScan) : DatabaseRecoveryMutation
    data class RecoveryTypeToggled(val entryType: EntryType) : DatabaseRecoveryMutation
    data class RecoveryRestoreCompleted(val report: DatabaseRecoveryReport) : DatabaseRecoveryMutation
    data class RecoveryOperationFailed(val message: String) : DatabaseRecoveryMutation
    data object RecoveryResultCleared : DatabaseRecoveryMutation
}
