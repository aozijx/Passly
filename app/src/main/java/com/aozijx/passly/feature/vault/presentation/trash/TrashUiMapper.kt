package com.aozijx.passly.feature.vault.presentation.trash

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashBottomSheetUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashDeletedAtUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryListUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryRowOperation
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashEntryUiModel
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashErrorUiState
import com.aozijx.passly.feature.vault.presentation.trash.model.TrashSupportingTextUiState
import com.aozijx.passly.presentation.shared.entry.labelRes
import com.aozijx.passly.presentation.shared.entryvisual.EntryClassificationInput
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel

internal object TrashUiMapper {
    fun map(state: TrashUiState): TrashBottomSheetUiState {
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
                    TrashContentState.Empty -> TrashEntryListUiState.Empty
                    is TrashContentState.Items -> TrashEntryListUiState.Items(map(content.entries, state.operation))
                }
            }
        return TrashBottomSheetUiState(
            entries = entries,
            isBusy = state.isBusy,
            error = error,
        )
    }

    private fun map(
        entries: List<EntryListItem>,
        operation: TrashOperationState,
    ): List<TrashEntryUiModel> =
        entries.map { entry ->
            val supportingText =
                entry.username.takeIf(String::isNotBlank)
                    ?: entry.associatedDomain?.takeIf(String::isNotBlank)
                    ?: entry.associatedAppPackage?.takeIf(String::isNotBlank)
            TrashEntryUiModel(
                id = entry.id.value,
                version = entry.identity.version.value,
                operation =
                    if (operation is TrashOperationState.Entry && operation.entryId == entry.id.value) {
                        TrashEntryRowOperation.Running
                    } else {
                        TrashEntryRowOperation.Idle
                    },
                title = entry.title,
                entryTypeLabelRes = entry.entryType.labelRes,
                supportingText =
                    supportingText
                        ?.let(TrashSupportingTextUiState::Value)
                        ?: TrashSupportingTextUiState.Absent,
                deletedAt =
                    entry.deletedAt
                        ?.let(TrashDeletedAtUiState::Recorded)
                        ?: TrashDeletedAtUiState.Unavailable,
                icon =
                    vaultItemIconUiModel(
                        iconName = entry.icon.name,
                        customImagePath = entry.iconCustomPath,
                        associatedAppPackage = entry.associatedAppPackage,
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
                                            entry.title.takeIf {
                                                entry.associatedAppPackage != null && entry.associatedDomain == null
                                            },
                                        ),
                                ),
                            ),
                    ),
            )
        }
}
