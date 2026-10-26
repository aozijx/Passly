package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionMutation
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionReducer

internal sealed interface DetailTagEditorMutation : DetailMutation

internal sealed interface DetailFaviconEditorMutation : DetailMutation

internal sealed interface DetailMutation {
    data object StateCleared : DetailMutation
    data class AccessHistoryChanged(val enabled: Boolean) : DetailMutation
    data class SessionOpened(
        val snapshot: DetailSessionSnapshot,
    ) : DetailMutation
    data class EntryPresented(
        val presentation: DetailEntryPresentation,
    ) : DetailMutation

    data object TitleEditingStarted : DetailMutation
    data object TitleEditingCancelled : DetailMutation
    data class EditedTitleChanged(val value: String) : DetailMutation
    data class FieldEditingStarted(val key: DetailDraftKey, val initialValue: String) : DetailMutation
    data class FieldDraftChanged(val key: DetailDraftKey, val value: String) : DetailMutation
    data class FieldEditingCancelled(val key: DetailDraftKey) : DetailMutation
    data class RevealedFieldChanged(val key: FieldKey, val value: SensitiveValue?) : DetailMutation
    data object RevealedFieldsCleared : DetailMutation
    data class HistoryChanged(val entryId: EntryId, val history: List<EntryActivity>) : DetailMutation
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
    data class Revision(val mutation: DetailRevisionMutation) : DetailMutation

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
    data class FaviconEditorOpened(val source: DetailFaviconSource) : DetailFaviconEditorMutation
    data class FaviconSourceChanged(val source: DetailFaviconSource) : DetailFaviconEditorMutation
    data class FaviconTabChanged(val tab: DetailFaviconTab) : DetailFaviconEditorMutation
    data class FaviconSearchChanged(val value: String) : DetailFaviconEditorMutation
    data class FaviconImageUrlChanged(val value: String) : DetailFaviconEditorMutation
    data object FaviconProcessingStarted : DetailFaviconEditorMutation
    data class FaviconInputStaged(val path: String) : DetailFaviconEditorMutation
    data class FaviconSourcePromoted(val path: String) : DetailFaviconEditorMutation
    data class FaviconProcessingFailed(val error: DetailFaviconProcessingError) : DetailFaviconEditorMutation
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

            is DetailMutation.SessionOpened -> mutation.snapshot.let { snapshot ->
                val presentation = snapshot.presentation
                state.copy(
                    entry = presentation.entry,
                    sections = presentation.sections,
                    associatedApps = presentation.associatedApps,
                    sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                    relatedEntries = snapshot.relatedEntries,
                    isEditingTitle = false,
                    editedTitle = presentation.entry.detailDisplayTitle,
                )
            }

            is DetailMutation.EntryPresented -> mutation.presentation.let { presentation ->
                state.copy(
                entry = presentation.entry,
                sections = presentation.sections,
                associatedApps = presentation.associatedApps,
                sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                editedTitle = if (state.isEditingTitle) {
                    state.editedTitle
                } else {
                    presentation.entry.detailDisplayTitle
                },
                )
            }

            DetailMutation.TitleEditingStarted -> state.copy(
                saveState = state.draftChanged(DetailEditCompletion.Title),
                isEditingTitle = true,
                editedTitle = state.entry?.detailDisplayTitle.orEmpty(),
            )

            DetailMutation.TitleEditingCancelled -> state.copy(
                saveState = state.draftChanged(DetailEditCompletion.Title),
                isEditingTitle = false,
                editedTitle = state.entry?.detailDisplayTitle.orEmpty(),
            )

            is DetailMutation.EditedTitleChanged -> state.copy(
                editedTitle = mutation.value,
                saveState = if (mutation.value == state.editedTitle) state.saveState else state.draftChanged(DetailEditCompletion.Title),
            )
            is DetailMutation.FieldEditingStarted -> state.copy(
                fieldEdits = state.fieldEdits.start(mutation.key, mutation.initialValue),
                saveState = state.draftChanged(mutation.key.completion()),
            )
            is DetailMutation.FieldDraftChanged -> state.copy(
                fieldEdits = state.fieldEdits.update(mutation.key, mutation.value),
                saveState = if (state.fieldEdits.isEditing(mutation.key) && state.fieldEdits.draft(mutation.key) != mutation.value) {
                    state.draftChanged(mutation.key.completion())
                } else state.saveState,
            )
            is DetailMutation.FieldEditingCancelled -> state.copy(
                fieldEdits = state.fieldEdits.finish(mutation.key),
                saveState = state.draftChanged(mutation.key.completion()),
            )
            is DetailMutation.RevealedFieldChanged -> state.copy(
                revealedFields = if (mutation.value == null) {
                    state.revealedFields - mutation.key
                } else {
                    state.revealedFields + (mutation.key to mutation.value)
                },
            )

            DetailMutation.RevealedFieldsCleared -> state.copy(revealedFields = emptyMap())
            is DetailMutation.HistoryChanged -> if (state.entry?.id == mutation.entryId) {
                state.copy(history = mutation.history)
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
                saveState = DetailSaveState.Saving(mutation.completion),
                faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                    when (val editor = state.faviconEditor) {
                        DetailFaviconEditorState.Closed -> editor
                        is DetailFaviconEditorState.Open -> editor.copy(
                            work = if (editor.work is DetailFaviconWorkState.Failed) {
                                DetailFaviconWorkState.Idle
                            } else {
                                editor.work
                            },
                        )
                    }
                } else {
                    state.faviconEditor
                },
            )

            is DetailMutation.SaveSucceeded -> {
                val saving = state.saveState
                if (saving !is DetailSaveState.Saving || saving.completion != mutation.completion) {
                    state
                } else {
                    val finishDraft = saving.draft == DetailSaveDraft.Unchanged
                    val finishTitle = finishDraft && mutation.completion == DetailEditCompletion.Title
                    state.copy(
                        isEditingTitle = state.isEditingTitle && !finishTitle,
                        editedTitle = if (finishTitle) state.entry?.detailDisplayTitle ?: state.editedTitle else state.editedTitle,
                        saveState = DetailSaveState.Idle,
                        fieldEdits = if (!finishDraft) state.fieldEdits else when (mutation.completion) {
                            is DetailEditCompletion.Field ->
                                state.fieldEdits.finish(DetailDraftKey.EntryField(mutation.completion.key))
                            DetailEditCompletion.Notes ->
                                state.fieldEdits.finish(DetailDraftKey.Notes)
                            DetailEditCompletion.Domain ->
                                state.fieldEdits.finish(DetailDraftKey.Domain)
                            else -> state.fieldEdits
                        },
                        tagEditor = if (mutation.completion == DetailEditCompletion.Tags) {
                            DetailTagEditorState.Closed
                        } else {
                            state.tagEditor
                        },
                        faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                            DetailFaviconEditorState.Closed
                        } else {
                            state.faviconEditor
                        },
                    )
                }
            }

            is DetailMutation.SaveFailed -> {
                val saving = state.saveState
                if (saving !is DetailSaveState.Saving || saving.completion != mutation.completion) {
                    state
                } else {
                    state.copy(
                        saveState = DetailSaveState.Failed(
                            mutation.completion,
                            mutation.errorCode,
                        ),
                        faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                            when (val editor = state.faviconEditor) {
                                DetailFaviconEditorState.Closed -> editor
                                is DetailFaviconEditorState.Open -> editor.copy(
                                    work = editor.work as? DetailFaviconWorkState.Failed
                                        ?: DetailFaviconWorkState.Failed(
                                            DetailFaviconProcessingError.SAVE_FAILED,
                                        ),
                                )
                            }
                        } else {
                            state.faviconEditor
                        },
                    )
                }
            }

            is DetailMutation.Revision -> state.copy(
                revisions = DetailRevisionReducer.reduce(state.revisions, mutation.mutation),
            )

            is DetailTagEditorMutation -> if (state.saveState == DetailSaveState.Saving(DetailEditCompletion.Tags)) {
                state
            } else state.copy(
                tagEditor = DetailTagEditorReducer.reduce(state.tagEditor, mutation),
            )

            is DetailFaviconEditorMutation -> state.copy(
                faviconEditor = DetailFaviconEditorReducer.reduce(state.faviconEditor, mutation),
            )
        }

    private fun DetailUiState.draftChanged(completion: DetailEditCompletion): DetailSaveState =
        when (val saving = saveState) {
            is DetailSaveState.Saving -> if (saving.completion == completion) {
                saving.copy(draft = DetailSaveDraft.Changed)
            } else saving
            else -> saving
        }

    private fun DetailDraftKey.completion(): DetailEditCompletion = when (this) {
        DetailDraftKey.Notes -> DetailEditCompletion.Notes
        DetailDraftKey.Domain -> DetailEditCompletion.Domain
        is DetailDraftKey.EntryField -> DetailEditCompletion.Field(fieldKey)
    }
}
