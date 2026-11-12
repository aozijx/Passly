package com.aozijx.passly.feature.vault.presentation.trash.model

import androidx.annotation.StringRes
import com.aozijx.passly.presentation.shared.entryvisual.VaultItemIconUiModel

internal data class TrashEntryUiModel(
    val id: String,
    val version: Int,
    val operation: TrashEntryRowOperation,
    val title: String,
    @field:StringRes val entryTypeLabelRes: Int,
    val supportingText: TrashSupportingTextUiState,
    val deletedAt: TrashDeletedAtUiState,
    val icon: VaultItemIconUiModel,
)

internal sealed interface TrashSupportingTextUiState {
    data object Absent : TrashSupportingTextUiState

    data class Value(
        val text: String,
    ) : TrashSupportingTextUiState
}

internal sealed interface TrashDeletedAtUiState {
    data object Unavailable : TrashDeletedAtUiState

    data class Recorded(
        val epochMillis: Long,
    ) : TrashDeletedAtUiState
}

internal enum class TrashEntryRowOperation { Idle, Running }

internal sealed interface TrashErrorUiState {
    data object Hidden : TrashErrorUiState

    data class OperationFailed(
        @field:StringRes val messageResource: Int,
    ) : TrashErrorUiState
}

internal sealed interface TrashEntryListUiState {
    data object Loading : TrashEntryListUiState

    data object Emptying : TrashEntryListUiState

    data class LoadFailed(
        @field:StringRes val messageResource: Int,
    ) : TrashEntryListUiState

    data object Empty : TrashEntryListUiState

    data class Items(
        val entries: List<TrashEntryUiModel>,
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
