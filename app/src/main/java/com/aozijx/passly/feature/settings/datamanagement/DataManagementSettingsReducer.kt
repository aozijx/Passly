package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.data.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.database.model.DatabaseRecoveryScan
import com.aozijx.passly.domain.entry.model.EntryType

internal sealed interface DataManagementSettingsMutation {
    data class SettingsChanged(
        val autoDownloadIcons: Boolean,
        val directoryUri: String?,
    ) : DataManagementSettingsMutation
    data class TrashLoaded(val entries: List<EntryListItem>) : DataManagementSettingsMutation
    data class TrashLoadFailed(val message: String) : DataManagementSettingsMutation
    data object TrashErrorCleared : DataManagementSettingsMutation
    data class TrashEntryActionStarted(val entryId: String) : DataManagementSettingsMutation
    data object TrashEntryActionFinished : DataManagementSettingsMutation
    data object EmptyTrashStarted : DataManagementSettingsMutation
    data object EmptyTrashFinished : DataManagementSettingsMutation
    data class TrashActionFailed(val message: String) : DataManagementSettingsMutation
    data class RecoveryPackagesLoaded(
        val packages: List<DatabaseRecoveryPackage>,
    ) : DataManagementSettingsMutation
    data class RecoveryOperationStarted(val packageId: String) : DataManagementSettingsMutation
    data class RecoveryScanCompleted(val scan: DatabaseRecoveryScan) : DataManagementSettingsMutation
    data class RecoveryTypeToggled(val entryType: EntryType) : DataManagementSettingsMutation
    data class RecoveryRestoreCompleted(val report: DatabaseRecoveryReport) : DataManagementSettingsMutation
    data class RecoveryOperationFailed(val message: String) : DataManagementSettingsMutation
    data object RecoveryResultCleared : DataManagementSettingsMutation
}

internal object DataManagementSettingsReducer {
    fun reduce(
        state: DataManagementSettingsUiState,
        mutation: DataManagementSettingsMutation,
    ): DataManagementSettingsUiState = when (mutation) {
        is DataManagementSettingsMutation.SettingsChanged -> state.copy(
            isAutoDownloadIcons = mutation.autoDownloadIcons,
            directoryUri = mutation.directoryUri,
        )
        is DataManagementSettingsMutation.TrashLoaded -> state.copy(
            deletedEntries = mutation.entries,
            isTrashLoading = false,
        )
        is DataManagementSettingsMutation.TrashLoadFailed -> state.copy(
            isTrashLoading = false,
            trashError = mutation.message,
        )
        DataManagementSettingsMutation.TrashErrorCleared -> state.copy(trashError = null)
        is DataManagementSettingsMutation.TrashEntryActionStarted -> state.copy(
            activeTrashEntryId = mutation.entryId,
            trashError = null,
        )
        DataManagementSettingsMutation.TrashEntryActionFinished ->
            state.copy(activeTrashEntryId = null)
        DataManagementSettingsMutation.EmptyTrashStarted -> state.copy(
            isEmptyingTrash = true,
            trashError = null,
        )
        DataManagementSettingsMutation.EmptyTrashFinished ->
            state.copy(isEmptyingTrash = false)
        is DataManagementSettingsMutation.TrashActionFailed ->
            state.copy(trashError = mutation.message)
        is DataManagementSettingsMutation.RecoveryPackagesLoaded -> state.copy(
            recoveryPackages = mutation.packages,
            isRecoveryLoading = false,
            activeRecoveryPackageId = null,
        )
        is DataManagementSettingsMutation.RecoveryOperationStarted -> state.copy(
            activeRecoveryPackageId = mutation.packageId,
            recoveryError = null,
            recoveryReport = null,
        )
        is DataManagementSettingsMutation.RecoveryScanCompleted -> state.copy(
            activeRecoveryPackageId = null,
            recoveryScan = mutation.scan,
            selectedRecoveryTypes = mutation.scan.recoverableByType.keys,
        )
        is DataManagementSettingsMutation.RecoveryTypeToggled -> state.copy(
            selectedRecoveryTypes = state.selectedRecoveryTypes.toMutableSet().apply {
                if (!add(mutation.entryType)) remove(mutation.entryType)
            },
        )
        is DataManagementSettingsMutation.RecoveryRestoreCompleted -> state.copy(
            activeRecoveryPackageId = null,
            recoveryReport = mutation.report,
            recoveryScan = null,
            selectedRecoveryTypes = emptySet(),
        )
        is DataManagementSettingsMutation.RecoveryOperationFailed -> state.copy(
            isRecoveryLoading = false,
            activeRecoveryPackageId = null,
            recoveryError = mutation.message,
        )
        DataManagementSettingsMutation.RecoveryResultCleared -> state.copy(
            recoveryScan = null,
            selectedRecoveryTypes = emptySet(),
            recoveryReport = null,
            recoveryError = null,
        )
    }
}
