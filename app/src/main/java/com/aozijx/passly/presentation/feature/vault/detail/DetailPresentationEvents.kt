package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailContentEvent
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEditorOverlayEvent
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFieldUiModel

internal fun DetailContentEvent.toDetailUiAction(): DetailUiAction? = when (this) {
    DetailContentEvent.EditIcon -> DetailUiAction.OpenFaviconEditor
    is DetailContentEvent.StartFieldEdit -> field.revealedKey?.let {
        DetailUiAction.StartFieldEdit(it, initialValue)
    }
    is DetailContentEvent.CancelFieldEdit -> field.revealedKey?.let(DetailUiAction::CancelFieldEdit)
    is DetailContentEvent.UpdateField -> field.revealedKey?.let {
        DetailUiAction.UpdateFieldDraft(it, value)
    }
    is DetailContentEvent.SaveField -> field.revealedKey?.let {
        DetailUiAction.SaveField(it, value)
    }
    is DetailContentEvent.ToggleFieldVisibility -> field.revealedKey?.let {
        DetailUiAction.ToggleFieldVisibility(it)
    }
    is DetailContentEvent.RevealFields -> fields.mapNotNullTo(linkedSetOf()) { it.revealedKey }
        .takeIf(Set<String>::isNotEmpty)
        ?.let(DetailUiAction::RevealFields)
    is DetailContentEvent.CopyField -> field.copyKey?.let(DetailUiAction::CopyField)
    DetailContentEvent.CopyOtpCode -> DetailUiAction.CopyOtpCode
    DetailContentEvent.ExportOtpQr -> DetailUiAction.ExportOtpQr
    is DetailContentEvent.OpenRelatedEntry -> null
    DetailContentEvent.OpenTagEditor -> DetailUiAction.OpenTagEditor
    DetailContentEvent.StartDomainEdit -> DetailUiAction.StartDomainEdit
    is DetailContentEvent.UpdateDomain -> DetailUiAction.UpdateDomainDraft(value)
    DetailContentEvent.SaveDomain -> DetailUiAction.SaveDomain
    DetailContentEvent.OpenPackagePicker -> DetailUiAction.LoadPackagePickerApps
    is DetailContentEvent.SelectAssociatedPackage -> DetailUiAction.SelectAssociatedPackage(packageName)
    DetailContentEvent.StartNotesEdit -> DetailUiAction.StartNotesEdit
    is DetailContentEvent.UpdateNotes -> DetailUiAction.UpdateNotesDraft(value)
    DetailContentEvent.SaveNotes -> DetailUiAction.SaveNotes
}

internal fun DetailEditorOverlayEvent.toDetailUiAction(): DetailUiAction? = when (this) {
    is DetailEditorOverlayEvent.UpdateTagInput -> DetailUiAction.UpdateTagInput(value)
    is DetailEditorOverlayEvent.SubmitTag -> DetailUiAction.SubmitTag(value)
    is DetailEditorOverlayEvent.RemoveTag -> DetailUiAction.RemoveTag(value)
    DetailEditorOverlayEvent.SaveTags -> DetailUiAction.SaveTags
    DetailEditorOverlayEvent.DismissTagEditor -> DetailUiAction.DismissTagEditor
    DetailEditorOverlayEvent.ConfirmDiscardTags -> DetailUiAction.ConfirmDiscardTags
    DetailEditorOverlayEvent.KeepEditingTags -> DetailUiAction.KeepEditingTags
    is DetailEditorOverlayEvent.SelectFaviconTab ->
        DetailUiAction.SelectFaviconTab(tab.toDetailFaviconTab())
    is DetailEditorOverlayEvent.UpdateFaviconSearch -> DetailUiAction.UpdateFaviconSearch(value)
    is DetailEditorOverlayEvent.SelectFaviconSource ->
        DetailUiAction.SelectFaviconSource(source.toDetailFaviconSource())
    DetailEditorOverlayEvent.UploadFavicon -> null
    is DetailEditorOverlayEvent.UpdateFaviconImageUrl -> DetailUiAction.UpdateFaviconImageUrl(value)
    DetailEditorOverlayEvent.DownloadFaviconImage -> DetailUiAction.DownloadFaviconImage
    DetailEditorOverlayEvent.SaveFavicon -> DetailUiAction.SaveFavicon
    DetailEditorOverlayEvent.DismissFaviconEditor -> DetailUiAction.DismissFaviconEditor
    DetailEditorOverlayEvent.ConfirmDiscardFavicon -> DetailUiAction.ConfirmDiscardFavicon
    DetailEditorOverlayEvent.KeepEditingFavicon -> DetailUiAction.KeepEditingFavicon
    is DetailEditorOverlayEvent.CropFavicon -> DetailUiAction.CropFaviconImage(zoom, offsetX, offsetY)
    DetailEditorOverlayEvent.UseFaviconWithoutCrop -> DetailUiAction.UseFaviconWithoutCrop
    DetailEditorOverlayEvent.CancelFaviconCrop -> DetailUiAction.CancelFaviconCrop
}

private val DetailFieldUiModel.revealedKey: String?
    get() = when (this) {
        DetailFieldUiModel.USERNAME -> RevealedFieldKey.USERNAME
        DetailFieldUiModel.PASSWORD -> RevealedFieldKey.PASSWORD
        DetailFieldUiModel.CARDHOLDER -> RevealedFieldKey.CARDHOLDER
        DetailFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
        DetailFieldUiModel.CARD_CVV -> RevealedFieldKey.CVV
        DetailFieldUiModel.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
        DetailFieldUiModel.SSH_PASSPHRASE -> RevealedFieldKey.SSH_PASSPHRASE
        DetailFieldUiModel.SSH_PRIVATE_KEY -> RevealedFieldKey.SSH_PRIVATE_KEY
        DetailFieldUiModel.SEED_PHRASE -> RevealedFieldKey.SEED_PHRASE
        DetailFieldUiModel.PASSKEY_DATA -> RevealedFieldKey.PASSKEY_DATA
        DetailFieldUiModel.ID_NUMBER -> RevealedFieldKey.ID_NUMBER
        DetailFieldUiModel.CARD_EXPIRATION,
        DetailFieldUiModel.WIFI_SSID,
        DetailFieldUiModel.HARDWARE_INFO,
            -> null
    }

private val DetailFieldUiModel.copyKey: FieldKey?
    get() = when (this) {
        DetailFieldUiModel.USERNAME -> FieldKey.USERNAME
        DetailFieldUiModel.PASSWORD -> FieldKey.PASSWORD
        DetailFieldUiModel.CARDHOLDER -> FieldKey.CARD_HOLDER
        DetailFieldUiModel.CARD_NUMBER -> FieldKey.CARD_NUMBER
        DetailFieldUiModel.CARD_CVV -> FieldKey.CARD_CVV
        DetailFieldUiModel.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        DetailFieldUiModel.CARD_EXPIRATION -> FieldKey.CARD_EXPIRATION
        DetailFieldUiModel.WIFI_SSID -> FieldKey.WIFI_SSID
        DetailFieldUiModel.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        DetailFieldUiModel.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        DetailFieldUiModel.SEED_PHRASE -> FieldKey.SEED_PHRASE
        DetailFieldUiModel.PASSKEY_DATA -> FieldKey.PASSKEY_DATA
        DetailFieldUiModel.HARDWARE_INFO -> FieldKey.HARDWARE_INFO
        DetailFieldUiModel.ID_NUMBER -> FieldKey.ID_NUMBER
    }
