package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
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
        val sections: List<DetailSectionKey>,
        val isEditingTitle: Boolean,
        val editedTitle: String,
    ) : DetailMutation

    data object TitleEditingStarted : DetailMutation
    data object TitleEditingCancelled : DetailMutation
    data class EditedTitleChanged(val value: String) : DetailMutation
    data class FieldEditingStarted(val key: String, val initialValue: String) : DetailMutation
    data class FieldDraftChanged(val key: String, val value: String) : DetailMutation
    data class FieldEditingCancelled(val key: String) : DetailMutation
    data class RevealedFieldChanged(val key: String, val value: SensitiveValue?) : DetailMutation
    data object RevealedFieldsCleared : DetailMutation
    data class SensitiveFieldPresenceChanged(
        val entryId: EntryId,
        val keys: Set<SensitiveFieldKey>,
    ) : DetailMutation
    data class HistoryChanged(val entryId: EntryId, val history: List<EntryActivity>) : DetailMutation
    data class RelatedEntriesChanged(val entryId: EntryId, val entries: List<Entry>) : DetailMutation
    data class AssociatedAppsChanged(
        val entryId: EntryId,
        val apps: List<DetailInstalledApp>,
    ) : DetailMutation
    data class PackagePickerAppsChanged(
        val entryId: EntryId,
        val apps: List<DetailInstalledApp>,
    ) : DetailMutation
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
                sections = mutation.sections,
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
            is DetailMutation.FieldEditingStarted -> state.copy(
                fieldEdits = state.fieldEdits.start(mutation.key, mutation.initialValue),
            )
            is DetailMutation.FieldDraftChanged -> state.copy(
                fieldEdits = state.fieldEdits.update(mutation.key, mutation.value),
            )
            is DetailMutation.FieldEditingCancelled -> state.copy(
                fieldEdits = state.fieldEdits.finish(mutation.key),
            )
            is DetailMutation.RevealedFieldChanged -> state.copy(
                revealedFields = if (mutation.value == null) {
                    state.revealedFields - mutation.key
                } else {
                    state.revealedFields + (mutation.key to mutation.value)
                },
            )

            DetailMutation.RevealedFieldsCleared -> state.copy(revealedFields = emptyMap())
            is DetailMutation.SensitiveFieldPresenceChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(sensitiveFieldKeys = mutation.keys)
            } else {
                state
            }

            is DetailMutation.HistoryChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(history = mutation.history)
            } else {
                state
            }
            is DetailMutation.RelatedEntriesChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(relatedEntries = mutation.entries)
            } else {
                state
            }
            is DetailMutation.AssociatedAppsChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(associatedApps = mutation.apps)
            } else {
                state
            }
            is DetailMutation.PackagePickerAppsChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(
                    packagePickerApps = mutation.apps,
                    packagePickerAppsLoaded = true,
                )
            } else {
                state
            }
            is DetailMutation.SaveStarted -> state.copy(
                savingEdit = mutation.completion,
                saveErrorCode = null,
                faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                    state.faviconEditor.copy(processingError = null)
                } else {
                    state.faviconEditor
                },
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
                        fieldEdits = when (mutation.completion) {
                            is DetailEditCompletion.SensitiveField ->
                                state.fieldEdits.finish(mutation.completion.key)
                            DetailEditCompletion.Notes ->
                                state.fieldEdits.finish(DetailEditKey.NOTES)
                            DetailEditCompletion.Associations ->
                                state.fieldEdits.finish(DetailEditKey.DOMAIN)
                            else -> state.fieldEdits
                        },
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
                        faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                            state.faviconEditor.copy(
                                processingError = state.faviconEditor.processingError
                                    ?: FaviconProcessingErrorUiModel.SAVE_FAILED,
                            )
                        } else {
                            state.faviconEditor
                        },
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
