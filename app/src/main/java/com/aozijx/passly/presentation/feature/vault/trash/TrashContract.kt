package com.aozijx.passly.presentation.feature.vault.trash

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.presentation.mapping.entry.toUiModel
import com.aozijx.passly.presentation.feature.vault.trash.ui.TrashEntryUiModel

data class TrashUiState(
    val content: TrashContentState = TrashContentState.Loading,
    val operation: TrashOperationState = TrashOperationState.Idle,
) {
    val isBusy: Boolean
        get() = operation is TrashOperationState.Entry ||
            operation == TrashOperationState.Emptying
}

sealed interface TrashContentState {
    data object Loading : TrashContentState
    data class Loaded(val entries: List<EntryListItem>) : TrashContentState
    data class Failed(val message: String) : TrashContentState
}

sealed interface TrashOperationState {
    data object Idle : TrashOperationState
    data class Entry(val entryId: String) : TrashOperationState
    data object Emptying : TrashOperationState
    data class Failed(val message: String) : TrashOperationState
}

internal fun TrashContentState.toUiModels(): List<TrashEntryUiModel> =
    (this as? TrashContentState.Loaded)?.entries.orEmpty().map { entry ->
        TrashEntryUiModel(
            id = entry.id.value,
            version = entry.identity.version.value,
            title = entry.title,
            entryType = entry.entryType.toUiModel(),
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
