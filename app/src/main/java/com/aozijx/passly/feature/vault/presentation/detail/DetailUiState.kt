package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey

data class DetailUiState(
    val snapshotState: DetailSnapshotState = DetailSnapshotState.Loading,
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<FieldKey, SensitiveValue> = emptyMap(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    val activityHistory: DetailActivityHistoryState = DetailActivityHistoryState.Loading,
    val historyFilter: DetailHistoryFilter = DetailHistoryFilter.ALL,
    val accountGroupEntries: List<Entry> = emptyList(),
    val otherRelatedEntries: List<Entry> = emptyList(),
    val relatedPasswords: List<RelatedPasswordEntry> = emptyList(),
    val passwordBinding: RelatedPasswordBindingUiState = RelatedPasswordBindingUiState.Closed,
    val passwordUnlink: RelatedPasswordUnlinkUiState = RelatedPasswordUnlinkUiState.Closed,
    val associatedApps: List<DetailInstalledApp> = emptyList(),
    val packagePickerApps: AppPackagePickerUiState<DetailInstalledApp> = AppPackagePickerUiState.NotRequested,
    val sections: List<DetailSectionKey> = emptyList(),
    val fieldEdits: DetailFieldEditState = DetailFieldEditState(),
    val saveState: DetailSaveState = DetailSaveState.Idle,
    val tagEditor: DetailTagEditorState = DetailTagEditorState.Closed,
    val faviconEditor: DetailFaviconEditorState = DetailFaviconEditorState.Closed,
    val revisions: DetailRevisionState = DetailRevisionState.Closed,
) {
    fun revealed(key: FieldKey): SensitiveValue? = revealedFields[key]
}

sealed interface DetailActivityHistoryState {
    data object Loading : DetailActivityHistoryState

    data object Failed : DetailActivityHistoryState

    data class Content(
        val activities: List<EntryActivity>,
        val phase: DetailActivityHistoryPhase,
    ) : DetailActivityHistoryState
}

enum class DetailActivityHistoryPhase { Refreshing, Ready, Failed }

internal val DetailActivityHistoryState.hasFailed: Boolean
    get() =
        when (this) {
            DetailActivityHistoryState.Loading -> false
            DetailActivityHistoryState.Failed -> true
            is DetailActivityHistoryState.Content -> phase == DetailActivityHistoryPhase.Failed
        }

internal fun DetailActivityHistoryState.refreshStarted(): DetailActivityHistoryState =
    when (this) {
        DetailActivityHistoryState.Loading,
        DetailActivityHistoryState.Failed,
        -> DetailActivityHistoryState.Loading

        is DetailActivityHistoryState.Content -> copy(phase = DetailActivityHistoryPhase.Refreshing)
    }

internal fun DetailActivityHistoryState.refreshFailed(): DetailActivityHistoryState =
    when (this) {
        DetailActivityHistoryState.Loading,
        DetailActivityHistoryState.Failed,
        -> DetailActivityHistoryState.Failed

        is DetailActivityHistoryState.Content -> copy(phase = DetailActivityHistoryPhase.Failed)
    }

enum class DetailHistoryFilter { ALL, CONTENT_CHANGES, LIFECYCLE, USAGE }

sealed interface DetailSnapshotState {
    data object Loading : DetailSnapshotState

    data object NotFound : DetailSnapshotState

    data object Failed : DetailSnapshotState

    data class Content(
        val entry: Entry,
        val refreshPhase: DetailSnapshotRefreshPhase = DetailSnapshotRefreshPhase.Current,
    ) : DetailSnapshotState
}

enum class DetailSnapshotRefreshPhase { Current, Refreshing, Failed }

internal fun DetailSnapshotState.entryOrNull(): Entry? =
    when (this) {
        DetailSnapshotState.Loading,
        DetailSnapshotState.NotFound,
        DetailSnapshotState.Failed,
        -> null

        is DetailSnapshotState.Content -> entry
    }

internal fun DetailSnapshotState.withEntry(entry: Entry): DetailSnapshotState =
    when (this) {
        DetailSnapshotState.Loading,
        DetailSnapshotState.NotFound,
        DetailSnapshotState.Failed,
        -> DetailSnapshotState.Content(entry)

        is DetailSnapshotState.Content -> copy(entry = entry)
    }

internal fun DetailSnapshotState.refreshStarted(): DetailSnapshotState =
    when (this) {
        is DetailSnapshotState.Content -> copy(refreshPhase = DetailSnapshotRefreshPhase.Refreshing)

        DetailSnapshotState.Loading,
        DetailSnapshotState.NotFound,
        DetailSnapshotState.Failed,
        -> this
    }

internal fun DetailSnapshotState.refreshFailed(): DetailSnapshotState =
    when (this) {
        is DetailSnapshotState.Content -> {
            if (refreshPhase == DetailSnapshotRefreshPhase.Refreshing) {
                copy(refreshPhase = DetailSnapshotRefreshPhase.Failed)
            } else {
                this
            }
        }

        DetailSnapshotState.Loading,
        DetailSnapshotState.NotFound,
        DetailSnapshotState.Failed,
        -> {
            this
        }
    }

internal val DetailSnapshotState.canRetryRefresh: Boolean
    get() = this is DetailSnapshotState.Content && refreshPhase == DetailSnapshotRefreshPhase.Failed

@ConsistentCopyVisibility
data class DetailFieldEditState private constructor(
    private val edits: Map<DetailDraftKey, DetailFieldEdit.Editing>,
) {
    constructor() : this(emptyMap())

    fun edit(key: DetailDraftKey): DetailFieldEdit = edits[key] ?: DetailFieldEdit.NotEditing

    fun start(
        key: DetailDraftKey,
        initialValue: String,
    ): DetailFieldEditState = copy(edits = edits + (key to DetailFieldEdit.Editing(initialValue, initialValue)))

    fun update(
        key: DetailDraftKey,
        value: String,
    ): DetailFieldEditState =
        edits[key]?.let { edit ->
            copy(edits = edits + (key to edit.copy(draft = value)))
        } ?: this

    fun rebase(
        key: DetailDraftKey,
        initialValue: String,
    ): DetailFieldEditState =
        edits[key]?.let { edit ->
            copy(edits = edits + (key to edit.copy(initialValue = initialValue)))
        } ?: this

    fun finish(key: DetailDraftKey): DetailFieldEditState = copy(edits = edits - key)

    override fun toString(): String = "DetailFieldEditState(editingKeys=${edits.keys})"
}

sealed interface DetailFieldEdit {
    data object NotEditing : DetailFieldEdit

    data class Editing(
        val initialValue: String,
        val draft: String,
    ) : DetailFieldEdit {
        override fun toString(): String = "DetailFieldEdit.Editing(***)"
    }
}

sealed interface DetailEditCompletion {
    sealed interface Draft : DetailEditCompletion

    sealed interface Other : DetailEditCompletion

    data object Title : Draft

    data object Notes : Draft

    data object Domain : Draft

    data class Field(
        val key: FieldKey,
    ) : Draft

    data object Favorite : Other

    data object Applications : Other

    data object Tags : Other

    data object Icon : Other
}

sealed interface DetailSaveState {
    data object Idle : DetailSaveState

    data class Saving(
        val completion: DetailEditCompletion,
        val draftChanged: Boolean = false,
    ) : DetailSaveState
}

sealed interface DetailDraftKey {
    data object Title : DetailDraftKey

    data object Notes : DetailDraftKey

    data object Domain : DetailDraftKey

    data class EntryField(
        val fieldKey: FieldKey,
    ) : DetailDraftKey
}
