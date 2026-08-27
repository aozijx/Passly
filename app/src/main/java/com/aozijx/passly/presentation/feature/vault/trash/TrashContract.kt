package com.aozijx.passly.presentation.feature.vault.trash

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.trash.TrashEntryUiModel

data class TrashUiState(
    val entries: List<EntryListItem> = emptyList(),
    val isLoading: Boolean = true,
    val activeEntryId: String? = null,
    val isEmptying: Boolean = false,
    val error: String? = null,
) {
    val isBusy: Boolean get() = activeEntryId != null || isEmptying
}

internal fun TrashUiState.toUiModels(): List<TrashEntryUiModel> = entries.map { entry ->
    TrashEntryUiModel(
        id = entry.id.value,
        version = entry.identity.version.value,
        title = entry.title,
        entryType = EntryTypeUiModel.valueOf(entry.entryType.name),
        username = entry.username,
        deletedAtEpochMs = entry.deletedAt,
        associatedDomain = entry.associatedDomain,
        associatedAppPackage = entry.associatedAppPackage,
        iconName = entry.icon.name,
        iconCustomPath = entry.iconCustomPath,
    )
}

sealed interface TrashUiAction {
    data class Restore(val entryId: String, val expectedVersion: Int) : TrashUiAction
    data class DeletePermanently(val entryId: String, val expectedVersion: Int) : TrashUiAction
    data object Empty : TrashUiAction
    data object ClearError : TrashUiAction
}
