package com.aozijx.passly.feature.vault.presentation.trash.model

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.shared.components.EntryVisualCategory
import com.aozijx.passly.presentation.shared.media.LocalIconImageModel

internal data class TrashEntryUiModel(
    val id: String,
    val version: Int,
    val title: String,
    val entryType: EntryType,
    val username: String,
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

    data class LoadFailed(
        val message: String,
    ) : TrashErrorUiState

    data class OperationFailed(
        val message: String,
    ) : TrashErrorUiState
}
