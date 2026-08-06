package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

data class DataManagementSettingsUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val deletedEntries: List<EntryListItem> = emptyList(),
    val isTrashLoading: Boolean = true,
    val activeTrashEntryId: String? = null,
    val isEmptyingTrash: Boolean = false,
    val trashError: String? = null
) {
    val isTrashBusy: Boolean
        get() = activeTrashEntryId != null || isEmptyingTrash
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
}
