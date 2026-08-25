package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetailState
import com.aozijx.passly.presentation.ui.vault.list.trash.TrashEntryUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultEntryTypeUiModel

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

internal fun DataManagementSettingsUiState.toDetailState() = DataManagementDetailState(
    isAutoDownloadIcons = isAutoDownloadIcons,
    deletedEntries = deletedEntries.map { entry ->
        TrashEntryUiModel(
            id = entry.id.value,
            version = entry.identity.version.value,
            title = entry.title,
            entryType = VaultEntryTypeUiModel.valueOf(entry.entryType.name),
            username = entry.username,
            deletedAtEpochMs = entry.deletedAt,
            associatedDomain = entry.associatedDomain,
            associatedAppPackage = entry.associatedAppPackage,
            iconName = entry.icon.name,
            iconCustomPath = entry.iconCustomPath,
        )
    },
    isTrashLoading = isTrashLoading,
    activeTrashEntryId = activeTrashEntryId,
    isEmptyingTrash = isEmptyingTrash,
    trashError = trashError,
)

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
