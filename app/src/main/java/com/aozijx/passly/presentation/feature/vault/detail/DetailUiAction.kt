package com.aozijx.passly.presentation.feature.vault.detail

import android.net.Uri
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel

sealed interface DetailUiAction {
    data class Initialize(val initialEntry: Entry) : DetailUiAction
    data class SyncEntry(val entry: Entry) : DetailUiAction
    data class CommitPatch(
        val patch: DetailEntryPatch,
        val completion: DetailEditCompletion,
    ) : DetailUiAction

    object StartTitleEdit : DetailUiAction
    object CancelTitleEdit : DetailUiAction
    data class UpdateEditedTitle(val value: String) : DetailUiAction

    object SaveTitle : DetailUiAction
    object ToggleFavorite : DetailUiAction

    data class RevealField(val key: String, val value: SensitiveValue?) : DetailUiAction
    data class ToggleVisibility(val key: String) : DetailUiAction
    data class SaveField(val key: String, val newValue: String) : DetailUiAction
    data class RevealHighSensitivityField(val key: String) : DetailUiAction
    data class RevealHighSensitivityFields(val keys: Set<String>) : DetailUiAction
    data object OpenTagEditor : DetailUiAction
    data class UpdateTagInput(val value: String) : DetailUiAction
    data class SubmitTag(val value: String) : DetailUiAction
    data class RemoveTag(val value: String) : DetailUiAction
    data object SaveTags : DetailUiAction
    data object DismissTagEditor : DetailUiAction
    data object ConfirmDiscardTags : DetailUiAction
    data object KeepEditingTags : DetailUiAction
    data object OpenFaviconEditor : DetailUiAction
    data class SelectFaviconSource(val source: FaviconDraftSourceUiModel) : DetailUiAction
    data class SelectFaviconTab(val tab: FaviconEditorTabUiModel) : DetailUiAction
    data class UpdateFaviconSearch(val value: String) : DetailUiAction
    data class UpdateFaviconImageUrl(val value: String) : DetailUiAction
    data class PickedFaviconImage(val uri: Uri) : DetailUiAction
    data object DownloadFaviconImage : DetailUiAction
    data object UseFaviconWithoutCrop : DetailUiAction
    data class CropFaviconImage(
        val zoom: Float,
        val offsetX: Float,
        val offsetY: Float,
    ) : DetailUiAction
    data object CancelFaviconCrop : DetailUiAction
    data object SaveFavicon : DetailUiAction
    data object DismissFaviconEditor : DetailUiAction
    data object ConfirmDiscardFavicon : DetailUiAction
    data object KeepEditingFavicon : DetailUiAction
    data class RecordAction(val field: String, val type: ActivityType) : DetailUiAction
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailUiAction
    object ClearSensitiveState : DetailUiAction
}
