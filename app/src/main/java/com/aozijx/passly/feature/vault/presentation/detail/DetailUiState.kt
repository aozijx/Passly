package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordBindingUiState

data class DetailUiState(
    val snapshotState: DetailSnapshotState = DetailSnapshotState.Loading,
    val entry: Entry? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<FieldKey, SensitiveValue> = emptyMap(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    val history: List<EntryActivity> = emptyList(),
    val activityHistory: DetailActivityHistoryState = DetailActivityHistoryState.Loading,
    val relatedEntries: List<Entry> = emptyList(),
    val passwordBinding: RelatedPasswordBindingUiState = RelatedPasswordBindingUiState.Closed,
    val associatedApps: List<DetailInstalledApp> = emptyList(),
    val packagePickerApps: List<DetailInstalledApp> = emptyList(),
    val packagePickerAppsLoaded: Boolean = false,
    val packagePickerAppsLoadFailed: Boolean = false,
    val sections: List<DetailSectionKey> = emptyList(),
    val fieldEdits: DetailFieldEditState = DetailFieldEditState(),
    val saveState: DetailSaveState = DetailSaveState.Idle,
    val tagEditor: DetailTagEditorState = DetailTagEditorState.Closed,
    val faviconEditor: DetailFaviconEditorState = DetailFaviconEditorState.Closed,
    val revisions: DetailRevisionState = DetailRevisionState.Closed,
) {
    fun revealed(key: FieldKey): SensitiveValue? = revealedFields[key]
}

enum class DetailActivityHistoryState { Loading, Ready, Failed }

enum class DetailSnapshotState { Loading, Ready, NotFound, Failed, RefreshingAfterRestore, RefreshFailedAfterRestore }

data class DetailFieldEditState(
    private val drafts: Map<DetailDraftKey, String> = emptyMap(),
) {
    fun isEditing(key: DetailDraftKey): Boolean = key in drafts

    fun draft(key: DetailDraftKey): String = drafts[key].orEmpty()

    fun start(
        key: DetailDraftKey,
        initialValue: String,
    ): DetailFieldEditState = copy(drafts = drafts + (key to initialValue))

    fun update(
        key: DetailDraftKey,
        value: String,
    ): DetailFieldEditState = if (key in drafts) copy(drafts = drafts + (key to value)) else this

    fun finish(key: DetailDraftKey): DetailFieldEditState = copy(drafts = drafts - key)

    override fun toString(): String = "DetailFieldEditState(editingKeys=${drafts.keys})"
}

sealed interface DetailEditCompletion {
    data object Title : DetailEditCompletion

    data object Favorite : DetailEditCompletion

    data object Notes : DetailEditCompletion

    data object Domain : DetailEditCompletion

    data object Applications : DetailEditCompletion

    data object Tags : DetailEditCompletion

    data object Icon : DetailEditCompletion

    data class Field(
        val key: FieldKey,
    ) : DetailEditCompletion
}

sealed interface DetailSaveState {
    data object Idle : DetailSaveState

    data class Saving(
        val completion: DetailEditCompletion,
        val draft: DetailSaveDraft = DetailSaveDraft.Unchanged,
    ) : DetailSaveState

    data class Failed(
        val completion: DetailEditCompletion,
        val errorCode: String,
    ) : DetailSaveState
}

enum class DetailSaveDraft { Unchanged, Changed }

sealed interface DetailDraftKey {
    data object Notes : DetailDraftKey

    data object Domain : DetailDraftKey

    data class EntryField(
        val fieldKey: FieldKey,
    ) : DetailDraftKey
}
