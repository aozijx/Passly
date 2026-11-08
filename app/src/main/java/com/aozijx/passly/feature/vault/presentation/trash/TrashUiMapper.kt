package com.aozijx.passly.feature.vault.presentation.trash

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashBottomSheetUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryListUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryUiModel
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashErrorUiState
import com.aozijx.passly.presentation.shared.components.EntryClassificationInput
import com.aozijx.passly.presentation.shared.components.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.media.toLocalIconImageModel

internal object TrashUiMapper {
    fun map(state: TrashUiState): TrashBottomSheetUiState {
        val activeEntryId = (state.operation as? TrashOperationState.Entry)?.entryId
        val error =
            when (val operation = state.operation) {
                is TrashOperationState.Failed -> TrashErrorUiState.OperationFailed(operation.message.resourceId)
                TrashOperationState.Emptying,
                is TrashOperationState.Entry,
                TrashOperationState.Idle,
                -> TrashErrorUiState.Hidden
            }
        val entries =
            if (state.operation == TrashOperationState.Emptying) {
                TrashEntryListUiState.Emptying
            } else {
                when (val content = state.content) {
                    TrashContentState.Loading -> TrashEntryListUiState.Loading
                    is TrashContentState.Failed -> TrashEntryListUiState.LoadFailed(content.message.resourceId)
                    is TrashContentState.Loaded ->
                        map(content.entries)
                            .takeIf { it.isNotEmpty() }
                            ?.let { TrashEntryListUiState.Items(it, activeEntryId) }
                            ?: TrashEntryListUiState.Empty
                }
            }
        return TrashBottomSheetUiState(
            entries = entries,
            isBusy = state.isBusy,
            error = error,
        )
    }

    fun map(entries: List<EntryListItem>): List<TrashEntryUiModel> =
        entries.map { entry ->
            TrashEntryUiModel(
                id = entry.id.value,
                version = entry.identity.version.value,
                title = entry.title,
                entryType = entry.entryType,
                supportingText =
                    entry.username.takeIf(String::isNotBlank)
                        ?: entry.associatedDomain?.takeIf(String::isNotBlank)
                        ?: entry.associatedAppPackage?.takeIf(String::isNotBlank),
                deletedAtEpochMs = entry.deletedAt,
                associatedDomain = entry.associatedDomain,
                associatedAppPackage = entry.associatedAppPackage,
                iconName = entry.icon.name,
                iconCustomPath = entry.iconCustomPath,
                iconImageModel = toLocalIconImageModel(entry.iconCustomPath),
                visualCategory =
                    EntryVisualCategoryClassifier.classify(
                        EntryClassificationInput(
                            entryType = entry.entryType,
                            title = entry.title,
                            username = entry.username,
                            urls = setOfNotNull(entry.associatedDomain),
                            domains = setOfNotNull(entry.associatedDomain),
                            packageNames = setOfNotNull(entry.associatedAppPackage),
                            appNames =
                                setOfNotNull(
                                    entry.title.takeIf { entry.associatedAppPackage != null && entry.associatedDomain == null },
                                ),
                        ),
                    ),
            )
        }
}
