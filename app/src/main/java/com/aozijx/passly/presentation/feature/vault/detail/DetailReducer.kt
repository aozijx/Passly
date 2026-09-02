package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel

internal sealed interface DetailTagEditorMutation : DetailMutation

internal sealed interface DetailFaviconEditorMutation : DetailMutation

internal sealed interface DetailMutation {
    data object StateCleared : DetailMutation
    data class AccessHistoryChanged(val enabled: Boolean) : DetailMutation
    data class EntryPresented(
        val entry: Entry,
        val entryType: EntryType,
        val strategySummary: String,
        val validationError: String?,
        val strategyReady: Boolean,
        val isEditingTitle: Boolean,
        val editedTitle: String,
    ) : DetailMutation

    data object TitleEditingStarted : DetailMutation
    data object TitleEditingCancelled : DetailMutation
    data class EditedTitleChanged(val value: String) : DetailMutation
    data class RevealedFieldChanged(val key: String, val value: SensitiveValue?) : DetailMutation
    data object RevealedFieldsCleared : DetailMutation
    data class SensitiveFieldPresenceChanged(val keys: Set<SensitiveFieldKey>) : DetailMutation
    data class HistoryChanged(val history: List<EntryActivity>) : DetailMutation
    data class RelatedEntriesChanged(val entries: List<Entry>) : DetailMutation
    data class SaveStarted(val completion: DetailEditCompletion) : DetailMutation
    data class SaveSucceeded(val completion: DetailEditCompletion) : DetailMutation
    data class SaveFailed(
        val completion: DetailEditCompletion,
        val errorCode: String,
    ) : DetailMutation

    data class TagEditorOpened(
        val currentTags: Set<String>,
        val availableTags: Set<String>,
    ) : DetailTagEditorMutation

    data class TagInputChanged(val value: String) : DetailTagEditorMutation
    data class TagSubmitted(val value: String) : DetailTagEditorMutation
    data class TagRemoved(val value: String) : DetailTagEditorMutation
    data object TagEditorDismissRequested : DetailTagEditorMutation
    data object TagEditorDiscardConfirmed : DetailTagEditorMutation
    data object TagEditorDiscardCancelled : DetailTagEditorMutation
    data class FaviconEditorOpened(val source: FaviconDraftSourceUiModel) : DetailFaviconEditorMutation
    data class FaviconSourceChanged(val source: FaviconDraftSourceUiModel) : DetailFaviconEditorMutation
    data class FaviconTabChanged(val tab: FaviconEditorTabUiModel) : DetailFaviconEditorMutation
    data class FaviconSearchChanged(val value: String) : DetailFaviconEditorMutation
    data class FaviconImageUrlChanged(val value: String) : DetailFaviconEditorMutation
    data object FaviconProcessingStarted : DetailFaviconEditorMutation
    data class FaviconInputStaged(val path: String) : DetailFaviconEditorMutation
    data class FaviconSourcePromoted(val path: String) : DetailFaviconEditorMutation
    data class FaviconProcessingFailed(val error: FaviconProcessingErrorUiModel) : DetailFaviconEditorMutation
    data object FaviconCropCancelled : DetailFaviconEditorMutation
    data object FaviconEditorDismissRequested : DetailFaviconEditorMutation
    data object FaviconEditorDiscardConfirmed : DetailFaviconEditorMutation
    data object FaviconEditorDiscardCancelled : DetailFaviconEditorMutation
}

internal object DetailReducer {
    fun reduce(state: DetailUiState, mutation: DetailMutation): DetailUiState =
        when (mutation) {
            DetailMutation.StateCleared -> DetailUiState()
            is DetailMutation.AccessHistoryChanged ->
                state.copy(isAccessHistoryEnabled = mutation.enabled)

            is DetailMutation.EntryPresented -> state.copy(
                entry = mutation.entry,
                entryType = mutation.entryType,
                strategySummary = mutation.strategySummary,
                validationError = mutation.validationError,
                strategyReady = mutation.strategyReady,
                isEditingTitle = mutation.isEditingTitle,
                editedTitle = mutation.editedTitle,
            )

            DetailMutation.TitleEditingStarted -> state.copy(
                isEditingTitle = true,
                editedTitle = state.entry?.title.orEmpty(),
            )

            DetailMutation.TitleEditingCancelled -> state.copy(
                isEditingTitle = false,
                editedTitle = state.entry?.title.orEmpty(),
            )

            is DetailMutation.EditedTitleChanged -> state.copy(editedTitle = mutation.value)
            is DetailMutation.RevealedFieldChanged -> state.copy(
                revealedFields = if (mutation.value == null) {
                    state.revealedFields - mutation.key
                } else {
                    state.revealedFields + (mutation.key to mutation.value)
                },
            )

            DetailMutation.RevealedFieldsCleared -> state.copy(revealedFields = emptyMap())
            is DetailMutation.SensitiveFieldPresenceChanged ->
                state.copy(sensitiveFieldKeys = mutation.keys)

            is DetailMutation.HistoryChanged -> state.copy(history = mutation.history)
            is DetailMutation.RelatedEntriesChanged ->
                state.copy(relatedEntries = mutation.entries)

            is DetailMutation.SaveStarted -> state.copy(
                savingEdit = mutation.completion,
                saveErrorCode = null,
            )

            is DetailMutation.SaveSucceeded -> {
                if (state.savingEdit != mutation.completion) {
                    state
                } else {
                    state.copy(
                        isEditingTitle = if (mutation.completion == DetailEditCompletion.Title) {
                            false
                        } else {
                            state.isEditingTitle
                        },
                        savingEdit = null,
                        completedEdit = mutation.completion,
                        saveCompletionId = state.saveCompletionId + 1,
                        saveErrorCode = null,
                        tagEditor = if (mutation.completion == DetailEditCompletion.Tags) {
                            DetailTagEditorUiModel()
                        } else {
                            state.tagEditor
                        },
                        faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                            DetailFaviconEditorUiModel()
                        } else {
                            state.faviconEditor
                        },
                    )
                }
            }

            is DetailMutation.SaveFailed -> {
                if (state.savingEdit != mutation.completion) {
                    state
                } else {
                    state.copy(
                        savingEdit = null,
                        saveErrorCode = mutation.errorCode,
                    )
                }
            }

            is DetailTagEditorMutation -> state.copy(
                tagEditor = DetailTagEditorReducer.reduce(state.tagEditor, mutation),
            )

            is DetailFaviconEditorMutation -> state.copy(
                faviconEditor = DetailFaviconEditorReducer.reduce(state.faviconEditor, mutation),
            )
        }
}
