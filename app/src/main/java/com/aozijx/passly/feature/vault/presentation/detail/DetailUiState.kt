package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.detail.DetailRelatedPassword
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey

data class DetailUiState(
    val snapshotState: DetailSnapshotState = DetailSnapshotState.Loading,
    val entry: Entry? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val isAccessHistoryEnabled: Boolean = false,
    val revealedFields: Map<FieldKey, SensitiveValue> = emptyMap(),
    val sensitiveFieldKeys: Set<SensitiveFieldKey> = emptySet(),
    val activityHistory: DetailActivityHistoryState = DetailActivityHistoryState.Loading(emptyList()),
    val activityFilter: DetailActivityFilter = DetailActivityFilter.ALL,
    val accountGroupEntries: List<Entry> = emptyList(),
    val otherRelatedEntries: List<Entry> = emptyList(),
    val relatedPasswords: List<DetailRelatedPassword> = emptyList(),
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
    val activities: List<EntryActivity>

    data class Loading(override val activities: List<EntryActivity>) : DetailActivityHistoryState

    data class Ready(override val activities: List<EntryActivity>) : DetailActivityHistoryState

    data class Failed(override val activities: List<EntryActivity>) : DetailActivityHistoryState
}

enum class DetailActivityFilter { ALL, ACTIONS, AUTOFILL }

enum class DetailSnapshotState { Loading, Ready, NotFound, Failed, RefreshingAfterRestore, RefreshFailedAfterRestore }

@ConsistentCopyVisibility
data class DetailFieldEditState private constructor(
    private val drafts: Map<DetailDraftKey, DetailFieldEditDraft>,
) {
    constructor() : this(emptyMap())

    fun isEditing(key: DetailDraftKey): Boolean = key in drafts

    fun draft(key: DetailDraftKey): String = drafts[key]?.value.orEmpty()

    fun initialValue(key: DetailDraftKey): String? = drafts[key]?.initialValue

    fun start(
        key: DetailDraftKey,
        initialValue: String,
    ): DetailFieldEditState = copy(drafts = drafts + (key to DetailFieldEditDraft(initialValue, initialValue)))

    fun update(
        key: DetailDraftKey,
        value: String,
    ): DetailFieldEditState =
        drafts[key]?.let { draft ->
            copy(drafts = drafts + (key to draft.copy(value = value)))
        } ?: this

    fun rebase(
        key: DetailDraftKey,
        initialValue: String,
    ): DetailFieldEditState =
        drafts[key]?.let { draft ->
            copy(drafts = drafts + (key to draft.copy(initialValue = initialValue)))
        } ?: this

    fun finish(key: DetailDraftKey): DetailFieldEditState = copy(drafts = drafts - key)

    override fun toString(): String = "DetailFieldEditState(editingKeys=${drafts.keys})"
}

private data class DetailFieldEditDraft(
    val initialValue: String,
    val value: String,
) {
    override fun toString() = "DetailFieldEditDraft(***)"
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
        val draftChanged: Boolean = false,
    ) : DetailSaveState
}

sealed interface DetailDraftKey {
    data object Notes : DetailDraftKey

    data object Domain : DetailDraftKey

    data class EntryField(
        val fieldKey: FieldKey,
    ) : DetailDraftKey
}
