package com.aozijx.passly.feature.vault.presentation.detail

import android.net.Uri
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

sealed interface DetailUiAction {
    data object StartTitleEdit : DetailUiAction

    data object CancelTitleEdit : DetailUiAction

    data class UpdateEditedTitle(
        val value: String,
    ) : DetailUiAction

    data object SaveTitle : DetailUiAction

    data object ToggleFavorite : DetailUiAction

    data object StartNotesEdit : DetailUiAction

    data class UpdateNotesDraft(
        val value: String,
    ) : DetailUiAction

    data object SaveNotes : DetailUiAction

    data object StartDomainEdit : DetailUiAction

    data class UpdateDomainDraft(
        val value: String,
    ) : DetailUiAction

    data object SaveDomain : DetailUiAction

    data object LoadPackagePickerApps : DetailUiAction

    data class PackagePickerSearchChanged(
        val query: String,
    ) : DetailUiAction

    data object RetryDetailLoad : DetailUiAction

    data object RetrySnapshotRefresh : DetailUiAction

    data class SelectAssociatedPackage(
        val packageName: String,
    ) : DetailUiAction

    data class StartFieldEdit(
        val field: DetailField,
        val initialValue: String,
    ) : DetailUiAction

    data class UpdateFieldDraft(
        val field: DetailField,
        val value: String,
    ) : DetailUiAction

    data class CancelFieldEdit(
        val field: DetailField,
    ) : DetailUiAction

    data class ToggleFieldVisibility(
        val field: DetailField,
    ) : DetailUiAction

    data object RevealBankCardFields : DetailUiAction

    data object RevealSshFields : DetailUiAction

    data class SaveField(
        val field: DetailField,
        val newValue: String,
    ) : DetailUiAction

    data object OpenTagEditor : DetailUiAction

    data class UpdateTagInput(
        val value: String,
    ) : DetailUiAction

    data class SubmitTag(
        val value: String,
    ) : DetailUiAction

    data class RemoveTag(
        val value: String,
    ) : DetailUiAction

    data object SaveTags : DetailUiAction

    data object DismissTagEditor : DetailUiAction

    data object ConfirmDiscardTags : DetailUiAction

    data object KeepEditingTags : DetailUiAction

    data object OpenFaviconEditor : DetailUiAction

    data class SelectFaviconSource(
        val source: DetailFaviconSource,
    ) : DetailUiAction

    data class SelectFaviconTab(
        val tab: DetailFaviconTab,
    ) : DetailUiAction

    data class UpdateFaviconSearch(
        val value: String,
    ) : DetailUiAction

    data class UpdateFaviconImageUrl(
        val value: String,
    ) : DetailUiAction

    data class PickedFaviconImage(
        val uri: Uri,
    ) : DetailUiAction

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

    data class CopyField(
        val field: DetailField,
    ) : DetailUiAction

    data object CopyOtpCode : DetailUiAction

    data object ShowEntryQr : DetailUiAction

    data object DismissEntryQr : DetailUiAction

    data class ToggleAccessHistoryRecording(
        val enabled: Boolean,
    ) : DetailUiAction

    data object ClearSensitiveState : DetailUiAction

    data object OpenRevisionHistory : DetailUiAction

    data object RetryRevisionHistory : DetailUiAction

    data object DismissRevisionHistory : DetailUiAction

    data class SelectRevision(
        val revisionId: String,
    ) : DetailUiAction

    data object RevisionBack : DetailUiAction

    data class RevealRevisionField(
        val key: SensitiveFieldKey,
    ) : DetailUiAction

    data object RequestRevisionRestore : DetailUiAction

    data object CancelRevisionRestore : DetailUiAction

    data object ConfirmRevisionRestore : DetailUiAction

    data object RetryActivityHistory : DetailUiAction

    data class ActivityFilterChanged(
        val filter: DetailActivityFilter,
    ) : DetailUiAction

    data object OpenPasswordBinding : DetailUiAction

    data object RetryPasswordBinding : DetailUiAction

    data class PasswordBindingSearchChanged(
        val query: String,
    ) : DetailUiAction

    data object DismissPasswordBinding : DetailUiAction

    data class RequestPasswordUnlink(
        val targetEntryId: String,
        val linkId: String,
    ) : DetailUiAction

    data object DismissPasswordUnlink : DetailUiAction

    data object ConfirmPasswordUnlink : DetailUiAction

    data class BindPassword(
        val targetEntryId: String,
    ) : DetailUiAction
}
