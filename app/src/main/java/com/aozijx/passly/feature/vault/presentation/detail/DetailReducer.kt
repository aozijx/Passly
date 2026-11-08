package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.common.search
import com.aozijx.passly.feature.vault.detail.DetailRelationsSnapshot
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionMutation
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionReducer

internal sealed interface DetailTagEditorMutation : DetailMutation

internal sealed interface DetailFaviconEditorMutation : DetailMutation

internal sealed interface DetailMutation {
    data object StateCleared : DetailMutation

    data class AccessHistoryChanged(
        val enabled: Boolean,
    ) : DetailMutation

    data class SessionOpened(
        val snapshot: DetailSessionSnapshot,
    ) : DetailMutation

    data object SnapshotNotFound : DetailMutation

    data object SnapshotLoadFailed : DetailMutation

    data object SnapshotRefreshStarted : DetailMutation

    data object SnapshotRefreshFailed : DetailMutation

    data class EntryPresented(
        val presentation: DetailEntryPresentation,
    ) : DetailMutation

    data object TitleEditingStarted : DetailMutation

    data object TitleEditingCancelled : DetailMutation

    data class EditedTitleChanged(
        val value: String,
    ) : DetailMutation

    data class FieldEditingStarted(
        val key: DetailDraftKey,
        val initialValue: String,
    ) : DetailMutation

    data class FieldDraftChanged(
        val key: DetailDraftKey,
        val value: String,
    ) : DetailMutation

    data class FieldEditingCancelled(
        val key: DetailDraftKey,
    ) : DetailMutation

    data class RevealedFieldChanged(
        val key: FieldKey,
        val value: SensitiveValue?,
    ) : DetailMutation

    data object RevealedFieldsCleared : DetailMutation

    data class HistoryChanged(
        val entryId: EntryId,
        val history: List<EntryActivity>,
    ) : DetailMutation

    data class HistoryLoadStarted(
        val entryId: EntryId,
    ) : DetailMutation

    data class HistoryLoadFailed(
        val entryId: EntryId,
    ) : DetailMutation

    data class ActivityFilterChanged(
        val filter: DetailActivityFilter,
    ) : DetailMutation

    data class PackagePickerAppsChanged(
        val entryId: EntryId,
        val apps: List<DetailInstalledApp>,
    ) : DetailMutation

    data class PackagePickerSearchChanged(
        val query: String,
    ) : DetailMutation

    data class PackagePickerAppsLoadStarted(
        val entryId: EntryId,
    ) : DetailMutation

    data class PackagePickerAppsFailed(
        val entryId: EntryId,
    ) : DetailMutation

    data class PasswordBindingStateChanged(
        val state: RelatedPasswordBindingUiState,
    ) : DetailMutation

    data class PasswordUnlinkStateChanged(
        val state: RelatedPasswordUnlinkUiState,
    ) : DetailMutation

    data class PasswordBindingSearchChanged(
        val query: String,
    ) : DetailMutation

    data class RelationsChanged(
        val entryId: EntryId,
        val relations: DetailRelationsSnapshot,
    ) : DetailMutation

    data class SaveStarted(
        val completion: DetailEditCompletion,
    ) : DetailMutation

    data class SaveSucceeded(
        val completion: DetailEditCompletion,
        val savedFieldValue: String? = null,
    ) : DetailMutation {
        override fun toString() = "SaveSucceeded($completion, ***)"
    }

    data class SaveFailed(
        val completion: DetailEditCompletion,
    ) : DetailMutation

    data class Revision(
        val mutation: DetailRevisionMutation,
    ) : DetailMutation

    data class TagEditorOpened(
        val currentTags: Set<String>,
        val availableTags: Set<String>,
    ) : DetailTagEditorMutation

    data class TagInputChanged(
        val value: String,
    ) : DetailTagEditorMutation

    data class TagSubmitted(
        val value: String,
    ) : DetailTagEditorMutation

    data class TagRemoved(
        val value: String,
    ) : DetailTagEditorMutation

    data object TagEditorDismissRequested : DetailTagEditorMutation

    data object TagEditorDiscardConfirmed : DetailTagEditorMutation

    data object TagEditorDiscardCancelled : DetailTagEditorMutation

    data class FaviconEditorOpened(
        val source: DetailFaviconSource,
    ) : DetailFaviconEditorMutation

    data class FaviconSourceChanged(
        val source: DetailFaviconSource,
    ) : DetailFaviconEditorMutation

    data class FaviconTabChanged(
        val tab: DetailFaviconTab,
    ) : DetailFaviconEditorMutation

    data class FaviconSearchChanged(
        val value: String,
    ) : DetailFaviconEditorMutation

    data class FaviconImageUrlChanged(
        val value: String,
    ) : DetailFaviconEditorMutation

    data object FaviconProcessingStarted : DetailFaviconEditorMutation

    data class FaviconInputStaged(
        val path: String,
    ) : DetailFaviconEditorMutation

    data class FaviconSourcePromoted(
        val path: String,
    ) : DetailFaviconEditorMutation

    data class FaviconProcessingFailed(
        val error: DetailFaviconProcessingError,
    ) : DetailFaviconEditorMutation

    data object FaviconCropCancelled : DetailFaviconEditorMutation

    data object FaviconEditorDismissRequested : DetailFaviconEditorMutation

    data object FaviconEditorDiscardConfirmed : DetailFaviconEditorMutation

    data object FaviconEditorDiscardCancelled : DetailFaviconEditorMutation
}

internal object DetailReducer {
    fun reduce(
        state: DetailUiState,
        mutation: DetailMutation,
    ): DetailUiState =
        when (mutation) {
            DetailMutation.StateCleared -> {
                DetailUiState()
            }

            is DetailMutation.AccessHistoryChanged -> {
                state.copy(isAccessHistoryEnabled = mutation.enabled)
            }

            is DetailMutation.PasswordBindingStateChanged -> state.copy(passwordBinding = mutation.state)
            is DetailMutation.PasswordUnlinkStateChanged -> state.copy(passwordUnlink = mutation.state)
            is DetailMutation.PasswordBindingSearchChanged -> {
                val picker = state.passwordBinding as? RelatedPasswordBindingUiState.Ready
                if (picker == null) {
                    state
                } else {
                    state.copy(passwordBinding = picker.copy(query = mutation.query, results = picker.candidates.toSearchResults(mutation.query)))
                }
            }

            is DetailMutation.RelationsChanged ->
                if (state.entry?.id == mutation.entryId) {
                    state.copy(
                        accountGroupEntries = mutation.relations.accountGroupEntries,
                        otherRelatedEntries = mutation.relations.otherRelatedEntries,
                        relatedPasswords = mutation.relations.relatedPasswords,
                    )
                } else {
                    state
                }

            is DetailMutation.SessionOpened -> {
                mutation.snapshot.let { snapshot ->
                    val presentation = snapshot.presentation
                    val entryChanged = state.entry?.id != presentation.entry.id
                    state.copy(
                        snapshotState = DetailSnapshotState.Ready,
                        entry = presentation.entry,
                        sections = presentation.sections,
                        associatedApps = presentation.associatedApps,
                        sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                        accountGroupEntries = snapshot.accountGroupEntries,
                        otherRelatedEntries = snapshot.otherRelatedEntries,
                        relatedPasswords = snapshot.relatedPasswords,
                        passwordUnlink = RelatedPasswordUnlinkUiState.Closed,
                        activityHistory =
                            if (entryChanged) {
                                DetailActivityHistoryState.Loading(emptyList())
                            } else {
                                state.activityHistory
                            },
                        isEditingTitle = false,
                        editedTitle = presentation.entry.detailDisplayTitle,
                    )
                }
            }

            DetailMutation.SnapshotNotFound -> {
                state.copy(snapshotState = DetailSnapshotState.NotFound)
            }

            DetailMutation.SnapshotLoadFailed -> {
                state.copy(snapshotState = DetailSnapshotState.Failed)
            }

            DetailMutation.SnapshotRefreshStarted -> {
                state.copy(snapshotState = DetailSnapshotState.RefreshingAfterRestore)
            }

            DetailMutation.SnapshotRefreshFailed -> {
                state.copy(snapshotState = DetailSnapshotState.RefreshFailedAfterRestore)
            }

            is DetailMutation.EntryPresented -> {
                mutation.presentation.let { presentation ->
                    val entryChanged = state.entry?.id != presentation.entry.id
                    state.copy(
                        entry = presentation.entry,
                        sections = presentation.sections,
                        associatedApps = presentation.associatedApps,
                        sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                        activityHistory =
                            if (entryChanged) {
                                DetailActivityHistoryState.Loading(emptyList())
                            } else {
                                state.activityHistory
                            },
                        passwordUnlink =
                            if (entryChanged) {
                                RelatedPasswordUnlinkUiState.Closed
                            } else {
                                state.passwordUnlink
                            },
                        editedTitle =
                            if (state.isEditingTitle) {
                                state.editedTitle
                            } else {
                                presentation.entry.detailDisplayTitle
                            },
                    )
                }
            }

            DetailMutation.TitleEditingStarted -> {
                state.copy(
                    saveProgress = state.draftChanged(DetailEditCompletion.Title),
                    isEditingTitle = true,
                    editedTitle = state.entry?.detailDisplayTitle.orEmpty(),
                )
            }

            DetailMutation.TitleEditingCancelled -> {
                state.copy(
                    saveProgress = state.draftChanged(DetailEditCompletion.Title),
                    isEditingTitle = false,
                    editedTitle = state.entry?.detailDisplayTitle.orEmpty(),
                )
            }

            is DetailMutation.EditedTitleChanged -> {
                state.copy(
                    editedTitle = mutation.value,
                    saveProgress =
                        if (mutation.value ==
                            state.editedTitle
                        ) {
                            state.saveProgress
                        } else {
                            state.draftChanged(DetailEditCompletion.Title)
                        },
                )
            }

            is DetailMutation.FieldEditingStarted -> {
                state.copy(
                    fieldEdits = state.fieldEdits.start(mutation.key, mutation.initialValue),
                    saveProgress = state.draftChanged(mutation.key.completion()),
                )
            }

            is DetailMutation.FieldDraftChanged -> {
                state.copy(
                    fieldEdits = state.fieldEdits.update(mutation.key, mutation.value),
                    saveProgress =
                        if (state.fieldEdits.isEditing(mutation.key) && state.fieldEdits.draft(mutation.key) != mutation.value) {
                            state.draftChanged(mutation.key.completion())
                        } else {
                            state.saveProgress
                        },
                )
            }

            is DetailMutation.FieldEditingCancelled -> {
                state.copy(
                    fieldEdits = state.fieldEdits.finish(mutation.key),
                    saveProgress = state.draftChanged(mutation.key.completion()),
                )
            }

            is DetailMutation.RevealedFieldChanged -> {
                state.copy(
                    revealedFields =
                        if (mutation.value == null) {
                            state.revealedFields - mutation.key
                        } else {
                            state.revealedFields + (mutation.key to mutation.value)
                        },
                )
            }

            DetailMutation.RevealedFieldsCleared -> {
                state.copy(revealedFields = emptyMap())
            }

            is DetailMutation.HistoryChanged -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(activityHistory = DetailActivityHistoryState.Ready(mutation.history))
                } else {
                    state
                }
            }

            is DetailMutation.HistoryLoadStarted -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(activityHistory = DetailActivityHistoryState.Loading(state.activityHistory.activities))
                } else {
                    state
                }
            }

            is DetailMutation.HistoryLoadFailed -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(activityHistory = DetailActivityHistoryState.Failed(state.activityHistory.activities))
                } else {
                    state
                }
            }

            is DetailMutation.ActivityFilterChanged -> state.copy(activityFilter = mutation.filter)

            is DetailMutation.PackagePickerAppsChanged -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(
                        packagePickerApps =
                            AppPackagePickerUiState.fromInstalledApps(mutation.apps),
                    )
                } else {
                    state
                }
            }

            is DetailMutation.PackagePickerSearchChanged -> {
                state.copy(
                    packagePickerApps =
                        state.packagePickerApps.search(
                            mutation.query,
                            DetailInstalledApp::label,
                            DetailInstalledApp::packageName,
                        ),
                )
            }

            is DetailMutation.PackagePickerAppsLoadStarted -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(packagePickerApps = AppPackagePickerUiState.Loading)
                } else {
                    state
                }
            }

            is DetailMutation.PackagePickerAppsFailed -> {
                if (state.entry?.id == mutation.entryId) {
                    state.copy(packagePickerApps = AppPackagePickerUiState.Failed)
                } else {
                    state
                }
            }

            is DetailMutation.SaveStarted -> {
                state.copy(
                    saveProgress = DetailSaveProgress(mutation.completion),
                    faviconEditor =
                        if (mutation.completion == DetailEditCompletion.Icon) {
                            when (val editor = state.faviconEditor) {
                                DetailFaviconEditorState.Closed -> {
                                    editor
                                }

                                is DetailFaviconEditorState.Open -> {
                                    editor.copy(
                                        work =
                                            if (editor.work is DetailFaviconWorkState.Failed) {
                                                DetailFaviconWorkState.Idle
                                            } else {
                                                editor.work
                                            },
                                    )
                                }
                            }
                        } else {
                            state.faviconEditor
                        },
                )
            }

            is DetailMutation.SaveSucceeded -> {
                val saving = state.saveProgress ?: return state
                if (saving.completion != mutation.completion) {
                    state
                } else {
                    val finishDraft = !saving.draftChanged
                    val finishTitle = finishDraft && mutation.completion == DetailEditCompletion.Title
                    state.copy(
                        isEditingTitle = state.isEditingTitle && !finishTitle,
                        editedTitle = if (finishTitle) state.entry?.detailDisplayTitle ?: state.editedTitle else state.editedTitle,
                        saveProgress = null,
                        fieldEdits =
                            if (finishDraft) {
                                when (mutation.completion) {
                                    is DetailEditCompletion.Field -> {
                                        state.fieldEdits.finish(DetailDraftKey.EntryField(mutation.completion.key))
                                    }

                                    DetailEditCompletion.Notes -> {
                                        state.fieldEdits.finish(DetailDraftKey.Notes)
                                    }

                                    DetailEditCompletion.Domain -> {
                                        state.fieldEdits.finish(DetailDraftKey.Domain)
                                    }

                                    else -> {
                                        state.fieldEdits
                                    }
                                }
                            } else if (mutation.completion is DetailEditCompletion.Field) {
                                mutation.savedFieldValue?.let { savedValue ->
                                    state.fieldEdits.rebase(
                                        DetailDraftKey.EntryField(mutation.completion.key),
                                        savedValue,
                                    )
                                } ?: state.fieldEdits
                            } else {
                                state.fieldEdits
                            },
                        tagEditor =
                            if (mutation.completion == DetailEditCompletion.Tags) {
                                DetailTagEditorState.Closed
                            } else {
                                state.tagEditor
                            },
                        faviconEditor =
                            if (mutation.completion == DetailEditCompletion.Icon) {
                                DetailFaviconEditorState.Closed
                            } else {
                                state.faviconEditor
                            },
                    )
                }
            }

            is DetailMutation.SaveFailed -> {
                val saving = state.saveProgress ?: return state
                if (saving.completion != mutation.completion) {
                    state
                } else {
                    state.copy(
                        saveProgress = null,
                        faviconEditor =
                            if (mutation.completion == DetailEditCompletion.Icon) {
                                when (val editor = state.faviconEditor) {
                                    DetailFaviconEditorState.Closed -> {
                                        editor
                                    }

                                    is DetailFaviconEditorState.Open -> {
                                        editor.copy(
                                            work =
                                                editor.work as? DetailFaviconWorkState.Failed
                                                    ?: DetailFaviconWorkState.Failed(
                                                        DetailFaviconProcessingError.SAVE_FAILED,
                                                    ),
                                        )
                                    }
                                }
                            } else {
                                state.faviconEditor
                            },
                    )
                }
            }

            is DetailMutation.Revision -> {
                state.copy(
                    revisions = DetailRevisionReducer.reduce(state.revisions, mutation.mutation),
                )
            }

            is DetailTagEditorMutation -> {
                if (state.saveProgress?.completion == DetailEditCompletion.Tags) {
                    state
                } else {
                    state.copy(
                        tagEditor = DetailTagEditorReducer.reduce(state.tagEditor, mutation),
                    )
                }
            }

            is DetailFaviconEditorMutation -> {
                state.copy(
                    faviconEditor = DetailFaviconEditorReducer.reduce(state.faviconEditor, mutation),
                )
            }
        }

    private fun DetailUiState.draftChanged(completion: DetailEditCompletion): DetailSaveProgress? =
        saveProgress?.let { progress ->
            progress.takeIf { it.completion != completion } ?: progress.copy(draftChanged = true)
        }

    private fun DetailDraftKey.completion(): DetailEditCompletion =
        when (this) {
            DetailDraftKey.Notes -> DetailEditCompletion.Notes
            DetailDraftKey.Domain -> DetailEditCompletion.Domain
            is DetailDraftKey.EntryField -> DetailEditCompletion.Field(fieldKey)
        }
}
