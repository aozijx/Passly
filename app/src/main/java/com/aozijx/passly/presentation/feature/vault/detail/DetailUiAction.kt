package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import android.net.Uri
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel

sealed interface DetailUiAction {
    data class CommitPatch(
        val patch: DetailEntryPatch,
        val completion: DetailEditCompletion,
    ) : DetailEntryAction

    data object StartTitleEdit : DetailEntryAction
    data object CancelTitleEdit : DetailEntryAction
    data class UpdateEditedTitle(val value: String) : DetailEntryAction

    data object SaveTitle : DetailEntryAction
    data object ToggleFavorite : DetailEntryAction

    data class ToggleFieldVisibility(val key: String) : DetailSensitiveAction
    data class RevealFields(val keys: Set<String>) : DetailSensitiveAction
    data class SaveField(val key: String, val newValue: String) : DetailSensitiveAction
    data object OpenTagEditor : DetailTagAction
    data class UpdateTagInput(val value: String) : DetailTagAction
    data class SubmitTag(val value: String) : DetailTagAction
    data class RemoveTag(val value: String) : DetailTagAction
    data object SaveTags : DetailTagAction
    data object DismissTagEditor : DetailTagAction
    data object ConfirmDiscardTags : DetailTagAction
    data object KeepEditingTags : DetailTagAction
    data object OpenFaviconEditor : DetailFaviconAction
    data class SelectFaviconSource(val source: FaviconDraftSourceUiModel) : DetailFaviconAction
    data class SelectFaviconTab(val tab: FaviconEditorTabUiModel) : DetailFaviconAction
    data class UpdateFaviconSearch(val value: String) : DetailFaviconAction
    data class UpdateFaviconImageUrl(val value: String) : DetailFaviconAction
    data class PickedFaviconImage(val uri: Uri) : DetailFaviconAction
    data object DownloadFaviconImage : DetailFaviconAction
    data object UseFaviconWithoutCrop : DetailFaviconAction
    data class CropFaviconImage(
        val zoom: Float,
        val offsetX: Float,
        val offsetY: Float,
    ) : DetailFaviconAction
    data object CancelFaviconCrop : DetailFaviconAction
    data object SaveFavicon : DetailFaviconAction
    data object DismissFaviconEditor : DetailFaviconAction
    data object ConfirmDiscardFavicon : DetailFaviconAction
    data object KeepEditingFavicon : DetailFaviconAction
    data class CopyField(val fieldKey: FieldKey) : DetailSensitiveAction
    data object CopyOtpCode : DetailSensitiveAction
    data object ExportOtpQr : DetailSensitiveAction
    data class ToggleAccessHistoryRecording(val enabled: Boolean) : DetailSensitiveAction
    data object ClearSensitiveState : DetailSensitiveAction
}

sealed interface DetailEntryAction : DetailUiAction
sealed interface DetailSensitiveAction : DetailUiAction
sealed interface DetailTagAction : DetailUiAction
sealed interface DetailFaviconAction : DetailUiAction
