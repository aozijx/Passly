package com.aozijx.passly.features.detail.contract

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.core.VaultEntry

data class DetailUiState(
    val entry: VaultEntry? = null,
    val vaultType: EntryType = EntryType.PASSWORD,
    val strategySummary: String = "",
    val validationError: String? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val strategyReady: Boolean = false
)

sealed interface DetailEvent {
    data class Initialize(val initialEntry: VaultEntry) : DetailEvent
    data class SyncEntry(val entry: VaultEntry) : DetailEvent
    data class CommitEntryUpdate(val entry: VaultEntry) : DetailEvent
    object ShowIconPicker : DetailEvent

    object StartTitleEdit : DetailEvent
    object CancelTitleEdit : DetailEvent
    data class UpdateEditedTitle(val value: String) : DetailEvent

    object SaveTitle : DetailEvent
    object ToggleFavorite : DetailEvent
}

sealed interface DetailEffect {
    data class EntryUpdated(val entry: VaultEntry) : DetailEffect
    data object IconPickerRequested : DetailEffect
}