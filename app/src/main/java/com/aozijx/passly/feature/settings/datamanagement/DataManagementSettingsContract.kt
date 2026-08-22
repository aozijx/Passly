package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.domain.entry.model.query.EntryListItem

data class DataManagementSettingsUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val deletedEntries: List<EntryListItem> = emptyList(),
    val isTrashLoading: Boolean = true,
    val activeTrashEntryId: String? = null,
    val isEmptyingTrash: Boolean = false,
    val trashError: String? = null,
) {
    val isTrashBusy: Boolean
        get() = activeTrashEntryId != null || isEmptyingTrash
}

sealed interface DataManagementSettingsUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataManagementSettingsUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataManagementSettingsUiAction
    data object ClearBackupDirectory : DataManagementSettingsUiAction
    data class RestoreTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataManagementSettingsUiAction

    data class DeleteTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataManagementSettingsUiAction

    data object EmptyTrash : DataManagementSettingsUiAction
    data object ClearTrashError : DataManagementSettingsUiAction
}
