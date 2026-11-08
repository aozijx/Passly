package com.aozijx.passly.feature.vault.presentation.trash.model

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel

internal data class TrashEntryUiModel(
    val id: String,
    val version: Int,
    val title: String,
    val entryType: EntryType,
    val supportingText: String?,
    val deletedAtEpochMs: Long?,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val iconName: String?,
    val iconCustomPath: String?,
    val iconImageModel: LocalIconImageModel?,
    val visualCategory: EntryVisualCategory,
)

internal sealed interface TrashErrorUiState {
    data object Hidden : TrashErrorUiState

    data class OperationFailed(@field:StringRes val messageResource: Int) : TrashErrorUiState
}

internal sealed interface TrashEntryListUiState {
    data object Loading : TrashEntryListUiState

    data object Emptying : TrashEntryListUiState

    data class LoadFailed(@field:StringRes val messageResource: Int) : TrashEntryListUiState

    data object Empty : TrashEntryListUiState

    data class Items(
        val entries: List<TrashEntryUiModel>,
        val activeEntryId: String?,
    ) : TrashEntryListUiState {
        init {
            require(entries.isNotEmpty())
        }
    }
}

internal data class TrashBottomSheetUiState(
    val entries: TrashEntryListUiState,
    val isBusy: Boolean,
    val error: TrashErrorUiState,
) {
    val canEmpty: Boolean
        get() = entries is TrashEntryListUiState.Items && !isBusy

    val entryCount: Int
        get() = (entries as? TrashEntryListUiState.Items)?.entries?.size ?: 0
}
