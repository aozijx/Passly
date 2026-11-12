package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.common.search
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionMutation
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionReducer

internal sealed interface DetailTagEditorMutation : DetailMutation

internal sealed interface DetailTagEditorEditMutation : DetailTagEditorMutation

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

    data class RevealedFieldAdded(
        val key: FieldKey,
        val value: SensitiveValue,
    ) : DetailMutation

    data class RevealedFieldCleared(
        val key: FieldKey,
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

    data class HistoryFilterChanged(
        val filter: DetailHistoryFilter,
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
        val relations: EntryRelationsSnapshot,
    ) : DetailMutation

    data class SaveStarted(
        val completion: DetailEditCompletion,
    ) : DetailMutation

    sealed interface SaveSucceeded : DetailMutation {
        val completion: DetailEditCompletion

        data class DraftSaved(
            override val completion: DetailEditCompletion.Draft,
            val persistedValue: String,
        ) : SaveSucceeded {
            override fun toString() = "SaveSucceeded.DraftSaved($completion, ***)"
        }

        data class OtherSaved(
            override val completion: DetailEditCompletion.Other,
        ) : SaveSucceeded
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
    ) : DetailTagEditorEditMutation

    data class TagSubmitted(
        val value: String,
    ) : DetailTagEditorEditMutation

    data class TagRemoved(
        val value: String,
    ) : DetailTagEditorEditMutation

    data object TagEditorDismissRequested : DetailTagEditorEditMutation

    data object TagEditorDiscardConfirmed : DetailTagEditorEditMutation

    data object TagEditorDiscardCancelled : DetailTagEditorEditMutation

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
        @get:StringRes val messageRes: Int,
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

            is DetailMutation.PasswordBindingStateChanged -> {
                state.copy(passwordBinding = mutation.state)
            }

            is DetailMutation.PasswordUnlinkStateChanged -> {
                state.copy(passwordUnlink = mutation.state)
            }

            is DetailMutation.PasswordBindingSearchChanged -> {
                val picker = state.passwordBinding as? RelatedPasswordBindingUiState.Ready
                if (picker == null) {
                    state
                } else {
                    state.copy(
                        passwordBinding = picker.copy(query = mutation.query),
                    )
                }
            }

            is DetailMutation.RelationsChanged -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(
                        accountGroupEntries = mutation.relations.accountGroupEntries,
                        otherRelatedEntries = mutation.relations.otherRelatedEntries,
                        relatedPasswords = mutation.relations.relatedPasswords,
                    )
                } else {
                    state
                }
            }

            is DetailMutation.SessionOpened -> {
                mutation.snapshot.let { snapshot ->
                    val presentation = snapshot.presentation
                    val entryChanged = state.snapshotState.entryOrNull()?.id != presentation.entry.id
                    state.copy(
                        snapshotState = DetailSnapshotState.Content(presentation.entry),
                        sections = presentation.sections,
                        associatedApps = presentation.associatedApps,
                        sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                        accountGroupEntries = snapshot.relations.accountGroupEntries,
                        otherRelatedEntries = snapshot.relations.otherRelatedEntries,
                        relatedPasswords = snapshot.relations.relatedPasswords,
                        passwordUnlink = RelatedPasswordUnlinkUiState.Closed,
                        activityHistory =
                            if (entryChanged) {
                                DetailActivityHistoryState.Loading
                            } else {
                                state.activityHistory
                            },
                        fieldEdits = state.fieldEdits.finish(DetailDraftKey.Title),
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
                state.copy(snapshotState = state.snapshotState.refreshStarted())
            }

            DetailMutation.SnapshotRefreshFailed -> {
                state.copy(snapshotState = state.snapshotState.refreshFailed())
            }

            is DetailMutation.EntryPresented -> {
                val presentation = mutation.presentation
                val entryChanged = state.snapshotState.entryOrNull()?.id != presentation.entry.id
                state.copy(
                    snapshotState = state.snapshotState.withEntry(presentation.entry),
                    sections = presentation.sections,
                    associatedApps = presentation.associatedApps,
                    sensitiveFieldKeys = presentation.sensitiveFieldKeys,
                    activityHistory =
                        if (entryChanged) {
                            DetailActivityHistoryState.Loading
                        } else {
                            state.activityHistory
                        },
                    passwordUnlink =
                        if (entryChanged) {
                            RelatedPasswordUnlinkUiState.Closed
                        } else {
                            state.passwordUnlink
                        },
                )
            }

            is DetailMutation.FieldEditingStarted -> {
                state.copy(
                    fieldEdits = state.fieldEdits.start(mutation.key, mutation.initialValue),
                    saveState = state.saveState.draftChanged(mutation.key.completion()),
                )
            }

            is DetailMutation.FieldDraftChanged -> {
                val edit = state.fieldEdits.edit(mutation.key)
                state.copy(
                    fieldEdits = state.fieldEdits.update(mutation.key, mutation.value),
                    saveState =
                        if (edit is DetailFieldEdit.Editing && edit.draft != mutation.value) {
                            state.saveState.draftChanged(mutation.key.completion())
                        } else {
                            state.saveState
                        },
                )
            }

            is DetailMutation.FieldEditingCancelled -> {
                state.copy(
                    fieldEdits = state.fieldEdits.finish(mutation.key),
                    saveState = state.saveState.draftChanged(mutation.key.completion()),
                )
            }

            is DetailMutation.RevealedFieldAdded -> {
                state.copy(revealedFields = state.revealedFields + (mutation.key to mutation.value))
            }

            is DetailMutation.RevealedFieldCleared -> {
                state.copy(revealedFields = state.revealedFields - mutation.key)
            }

            DetailMutation.RevealedFieldsCleared -> {
                state.copy(revealedFields = emptyMap())
            }

            is DetailMutation.HistoryChanged -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(
                        activityHistory =
                            DetailActivityHistoryState.Content(
                                activities = mutation.history,
                                phase = DetailActivityHistoryPhase.Ready,
                            ),
                    )
                } else {
                    state
                }
            }

            is DetailMutation.HistoryLoadStarted -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(activityHistory = state.activityHistory.refreshStarted())
                } else {
                    state
                }
            }

            is DetailMutation.HistoryLoadFailed -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(activityHistory = state.activityHistory.refreshFailed())
                } else {
                    state
                }
            }

            is DetailMutation.HistoryFilterChanged -> {
                state.copy(historyFilter = mutation.filter)
            }

            is DetailMutation.PackagePickerAppsChanged -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
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
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(packagePickerApps = AppPackagePickerUiState.Loading)
                } else {
                    state
                }
            }

            is DetailMutation.PackagePickerAppsFailed -> {
                if (state.snapshotState.entryOrNull()?.id == mutation.entryId) {
                    state.copy(packagePickerApps = AppPackagePickerUiState.Failed)
                } else {
                    state
                }
            }

            is DetailMutation.SaveStarted -> {
                state.copy(
                    saveState = DetailSaveState.Saving(mutation.completion),
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
                val saving =
                    when (val saveState = state.saveState) {
                        DetailSaveState.Idle -> return state
                        is DetailSaveState.Saving -> saveState
                    }
                if (saving.completion != mutation.completion) {
                    state
                } else {
                    val fieldEdits =
                        when (mutation) {
                            is DetailMutation.SaveSucceeded.DraftSaved -> {
                                val key = mutation.completion.draftKey()
                                if (saving.draftChanged) {
                                    state.fieldEdits.rebase(key, mutation.persistedValue)
                                } else {
                                    state.fieldEdits.finish(key)
                                }
                            }

                            is DetailMutation.SaveSucceeded.OtherSaved -> {
                                state.fieldEdits
                            }
                        }
                    state.copy(
                        saveState = DetailSaveState.Idle,
                        fieldEdits = fieldEdits,
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
                val saving =
                    when (val saveState = state.saveState) {
                        DetailSaveState.Idle -> return state
                        is DetailSaveState.Saving -> saveState
                    }
                if (saving.completion != mutation.completion) {
                    state
                } else {
                    state.copy(
                        saveState = DetailSaveState.Idle,
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
                                                        R.string.vault_detail_favicon_error_save_failed,
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
                if (
                    state.saveState is DetailSaveState.Saving &&
                    state.saveState.completion == DetailEditCompletion.Tags
                ) {
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

    private fun DetailSaveState.draftChanged(completion: DetailEditCompletion): DetailSaveState =
        when (this) {
            DetailSaveState.Idle -> {
                DetailSaveState.Idle
            }

            is DetailSaveState.Saving -> {
                if (this.completion != completion) {
                    this
                } else {
                    copy(draftChanged = true)
                }
            }
        }

    private fun DetailDraftKey.completion(): DetailEditCompletion =
        when (this) {
            DetailDraftKey.Title -> DetailEditCompletion.Title
            DetailDraftKey.Notes -> DetailEditCompletion.Notes
            DetailDraftKey.Domain -> DetailEditCompletion.Domain
            is DetailDraftKey.EntryField -> DetailEditCompletion.Field(fieldKey)
        }

    private fun DetailEditCompletion.Draft.draftKey(): DetailDraftKey =
        when (this) {
            DetailEditCompletion.Title -> DetailDraftKey.Title
            DetailEditCompletion.Notes -> DetailDraftKey.Notes
            DetailEditCompletion.Domain -> DetailDraftKey.Domain
            is DetailEditCompletion.Field -> DetailDraftKey.EntryField(key)
        }
}
