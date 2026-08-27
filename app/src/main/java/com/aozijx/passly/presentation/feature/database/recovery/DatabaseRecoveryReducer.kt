package com.aozijx.passly.presentation.feature.database.recovery

internal object DatabaseRecoveryReducer {
    fun reduce(
        state: DatabaseRecoveryUiState,
        mutation: DatabaseRecoveryMutation,
    ): DatabaseRecoveryUiState = when (mutation) {
        is DatabaseRecoveryMutation.RecoveryPackagesLoaded -> state.copy(
            recoveryPackages = mutation.packages,
            isRecoveryLoading = false,
            activeRecoveryPackageId = null,
        )
        is DatabaseRecoveryMutation.RecoveryOperationStarted -> state.copy(
            activeRecoveryPackageId = mutation.packageId,
            recoveryError = null,
            recoveryReport = null,
        )
        is DatabaseRecoveryMutation.RecoveryScanCompleted -> state.copy(
            activeRecoveryPackageId = null,
            recoveryScan = mutation.scan,
            selectedRecoveryTypes = mutation.scan.recoverableByType.keys,
        )
        is DatabaseRecoveryMutation.RecoveryTypeToggled -> state.copy(
            selectedRecoveryTypes = state.selectedRecoveryTypes.toMutableSet().apply {
                if (!add(mutation.entryType)) remove(mutation.entryType)
            },
        )
        is DatabaseRecoveryMutation.RecoveryRestoreCompleted -> state.copy(
            activeRecoveryPackageId = null,
            recoveryReport = mutation.report,
            recoveryScan = null,
            selectedRecoveryTypes = emptySet(),
        )
        is DatabaseRecoveryMutation.RecoveryOperationFailed -> state.copy(
            isRecoveryLoading = false,
            activeRecoveryPackageId = null,
            recoveryError = mutation.message,
            isClearingDatabase = false,
        )
        DatabaseRecoveryMutation.DatabaseClearStarted -> state.copy(
            isClearingDatabase = true,
            databaseCleared = false,
            recoveryError = null,
        )
        DatabaseRecoveryMutation.DatabaseClearCompleted -> state.copy(
            isClearingDatabase = false,
            databaseCleared = true,
        )
        DatabaseRecoveryMutation.RecoveryResultCleared -> state.copy(
            recoveryScan = null,
            selectedRecoveryTypes = emptySet(),
            recoveryReport = null,
            recoveryError = null,
            isClearingDatabase = false,
            databaseCleared = false,
        )
    }
}
