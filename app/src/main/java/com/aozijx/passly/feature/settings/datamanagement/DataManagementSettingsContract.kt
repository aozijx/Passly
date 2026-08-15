package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.EntryType

data class DataManagementSettingsUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val deletedEntries: List<EntryListItem> = emptyList(),
    val isTrashLoading: Boolean = true,
    val activeTrashEntryId: String? = null,
    val isEmptyingTrash: Boolean = false,
    val trashError: String? = null,
    val recoveryPackages: List<DatabaseRecoveryPackage> = emptyList(),
    val isRecoveryLoading: Boolean = true,
    val activeRecoveryPackageId: String? = null,
    val recoveryScan: DatabaseRecoveryScan? = null,
    val selectedRecoveryTypes: Set<EntryType> = emptySet(),
    val recoveryReport: DatabaseRecoveryReport? = null,
    val recoveryError: String? = null,
) {
    val isTrashBusy: Boolean
        get() = activeTrashEntryId != null || isEmptyingTrash

    val isRecoveryBusy: Boolean get() = activeRecoveryPackageId != null
}

sealed interface DataManagementSettingsAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataManagementSettingsAction
    data class SetBackupDirectoryUri(val uri: String) : DataManagementSettingsAction
    data object ClearBackupDirectory : DataManagementSettingsAction
    data class RestoreTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataManagementSettingsAction

    data class DeleteTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataManagementSettingsAction

    data object EmptyTrash : DataManagementSettingsAction
    data object ClearTrashError : DataManagementSettingsAction
    data object RefreshRecoveryPackages : DataManagementSettingsAction
    data class ScanRecoveryPackage(val packageId: String) : DataManagementSettingsAction
    data class ToggleRecoveryType(val entryType: EntryType) : DataManagementSettingsAction
    data class RestoreRecoveryPackage(val packageId: String) : DataManagementSettingsAction
    data class DeleteRecoveryPackage(val packageId: String) : DataManagementSettingsAction
    data object ClearRecoveryResult : DataManagementSettingsAction
}
