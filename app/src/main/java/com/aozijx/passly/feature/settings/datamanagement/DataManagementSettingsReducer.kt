package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem

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
    }
}
