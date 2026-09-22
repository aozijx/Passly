package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBodyUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailContentCallbacks
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlayCallbacks
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailSshUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconEditorTabUiModel

internal class DetailInteractionCallbacks(
    private val model: DetailBodyUiModel,
    private val dispatch: (DetailUiAction) -> Unit,
    private val openRelatedEntry: (String) -> Unit,
    private val dismissOtpQr: () -> Unit,
    private val uploadFavicon: () -> Unit,
) : DetailContentCallbacks, DetailEditorOverlayCallbacks {
    override fun onIconEditRequested() = dispatch(DetailUiAction.OpenFaviconEditor)

    override fun onCredentialEditingChanged(
        field: CredentialFieldUiModel,
        editing: Boolean,
        initialValue: String,
    ) = dispatch(
        if (editing) DetailUiAction.StartFieldEdit(field.toDetailField(), initialValue)
        else DetailUiAction.CancelFieldEdit(field.toDetailField()),
    )

    override fun onCredentialValueChanged(field: CredentialFieldUiModel, value: String) =
        onFieldValueChanged(field.toDetailField(), value)
    override fun onCredentialSaveRequested(field: CredentialFieldUiModel, value: String) =
        onFieldSaveRequested(field.toDetailField(), value)
    override fun onCredentialCopyRequested(field: CredentialFieldUiModel) =
        onFieldCopyRequested(field.toDetailField())
    override fun onCredentialRevealRequested(field: CredentialFieldUiModel) =
        onFieldRevealRequested(field.toDetailField())
    override fun onFieldEditStarted(field: DetailFieldUiModel, initialValue: String) =
        dispatch(DetailUiAction.StartFieldEdit(field, initialValue))

    override fun onFieldValueChanged(field: DetailFieldUiModel, value: String) =
        dispatch(DetailUiAction.UpdateFieldDraft(field, value))
    override fun onFieldSaveRequested(field: DetailFieldUiModel, value: String) =
        dispatch(DetailUiAction.SaveField(field, value))
    override fun onFieldCopyRequested(field: DetailFieldUiModel) =
        dispatch(DetailUiAction.CopyField(field))
    override fun onFieldRevealRequested(field: DetailFieldUiModel) =
        dispatch(DetailUiAction.ToggleFieldVisibility(field))

    override fun onBankCardValueChanged(field: DetailBankCardFieldUiModel, value: String) =
        onFieldValueChanged(field.toDetailField(), value)
    override fun onBankCardEditStarted(field: DetailBankCardFieldUiModel, value: String) =
        dispatch(DetailUiAction.StartFieldEdit(field.toDetailField(), value))
    override fun onBankCardSaveRequested(field: DetailBankCardFieldUiModel, value: String) =
        onFieldSaveRequested(field.toDetailField(), value)
    override fun onBankCardCopyRequested(field: DetailBankCardFieldUiModel) =
        onFieldCopyRequested(field.toDetailField())
    override fun onBankCardRevealRequested(field: DetailBankCardFieldUiModel) =
        onFieldRevealRequested(field.toDetailField())

    override fun onBankCardRevealAllRequested() {
        val card = model.bankCard ?: return
        reveal(card.fieldsToReveal())
    }

    override fun onSshRevealAllRequested() {
        val ssh = model.ssh ?: return
        reveal(ssh.fieldsToReveal())
    }

    private fun reveal(fields: Set<DetailFieldUiModel>) {
        if (fields.isNotEmpty()) dispatch(DetailUiAction.RevealFields(fields))
    }

    override fun onOtpQrRequested() = dispatch(DetailUiAction.ExportOtpQr)
    override fun onOtpCopyRequested() = dispatch(DetailUiAction.CopyOtpCode)
    override fun onOtpQrDismissed() = dismissOtpQr()
    override fun onRelatedEntryRequested(entryId: String) = openRelatedEntry(entryId)
    override fun onTagEditorRequested() = dispatch(DetailUiAction.OpenTagEditor)
    override fun onDomainEditStarted() = dispatch(DetailUiAction.StartDomainEdit)
    override fun onDomainChanged(value: String) = dispatch(DetailUiAction.UpdateDomainDraft(value))
    override fun onDomainSaveRequested(value: String) = dispatch(DetailUiAction.SaveDomain)
    override fun onPackagePickerRequested() = dispatch(DetailUiAction.LoadPackagePickerApps)
    override fun onPackageSelected(packageName: String) =
        dispatch(DetailUiAction.SelectAssociatedPackage(packageName))
    override fun onNotesEditStarted() = dispatch(DetailUiAction.StartNotesEdit)
    override fun onNotesChanged(value: String) = dispatch(DetailUiAction.UpdateNotesDraft(value))
    override fun onNotesSaveRequested(value: String) = dispatch(DetailUiAction.SaveNotes)

    override fun onTagInputChanged(value: String) = dispatch(DetailUiAction.UpdateTagInput(value))
    override fun onTagSubmitted(value: String) = dispatch(DetailUiAction.SubmitTag(value))
    override fun onTagRemoved(value: String) = dispatch(DetailUiAction.RemoveTag(value))
    override fun onTagsSaveRequested() = dispatch(DetailUiAction.SaveTags)
    override fun onTagEditorDismissed() = dispatch(DetailUiAction.DismissTagEditor)
    override fun onTagDiscardConfirmed() = dispatch(DetailUiAction.ConfirmDiscardTags)
    override fun onTagEditingContinued() = dispatch(DetailUiAction.KeepEditingTags)
    override fun onFaviconTabSelected(tab: FaviconEditorTabUiModel) =
        dispatch(DetailUiAction.SelectFaviconTab(tab))
    override fun onFaviconSearchChanged(value: String) =
        dispatch(DetailUiAction.UpdateFaviconSearch(value))
    override fun onFaviconSourceSelected(source: FaviconDraftSourceUiModel) =
        dispatch(DetailUiAction.SelectFaviconSource(source))
    override fun onFaviconUploadRequested() = uploadFavicon()
    override fun onFaviconImageUrlChanged(value: String) =
        dispatch(DetailUiAction.UpdateFaviconImageUrl(value))
    override fun onFaviconDownloadRequested() = dispatch(DetailUiAction.DownloadFaviconImage)
    override fun onFaviconSaveRequested() = dispatch(DetailUiAction.SaveFavicon)
    override fun onFaviconEditorDismissed() = dispatch(DetailUiAction.DismissFaviconEditor)
    override fun onFaviconDiscardConfirmed() = dispatch(DetailUiAction.ConfirmDiscardFavicon)
    override fun onFaviconEditingContinued() = dispatch(DetailUiAction.KeepEditingFavicon)
    override fun onFaviconCropRequested(zoom: Float, offsetX: Float, offsetY: Float) =
        dispatch(DetailUiAction.CropFaviconImage(zoom, offsetX, offsetY))
    override fun onFaviconUseWithoutCropRequested() =
        dispatch(DetailUiAction.UseFaviconWithoutCrop)
    override fun onFaviconCropCancelled() = dispatch(DetailUiAction.CancelFaviconCrop)
}

internal fun DetailBankCardUiModel.fieldsToReveal(): Set<DetailFieldUiModel> = buildSet {
    if (hasCardNumber && !cardNumberRevealed) add(DetailFieldUiModel.CARD_NUMBER)
    if (hasCvv && !cvvRevealed) add(DetailFieldUiModel.CARD_CVV)
    if (hasPaymentPin && !paymentPinRevealed) add(DetailFieldUiModel.PAYMENT_PIN)
    if (!cardholderRevealed) add(DetailFieldUiModel.CARDHOLDER)
}

internal fun DetailSshUiModel.fieldsToReveal(): Set<DetailFieldUiModel> = buildSet {
    if (hasPrivateKey && !privateKeyRevealed) add(DetailFieldUiModel.SSH_PRIVATE_KEY)
    if (hasPassphrase && !passphraseRevealed) add(DetailFieldUiModel.SSH_PASSPHRASE)
}

private fun CredentialFieldUiModel.toDetailField(): DetailFieldUiModel = when (this) {
    CredentialFieldUiModel.USERNAME -> DetailFieldUiModel.USERNAME
    CredentialFieldUiModel.PASSWORD -> DetailFieldUiModel.PASSWORD
}

private fun DetailBankCardFieldUiModel.toDetailField(): DetailFieldUiModel = when (this) {
    DetailBankCardFieldUiModel.CARDHOLDER -> DetailFieldUiModel.CARDHOLDER
    DetailBankCardFieldUiModel.CARD_NUMBER -> DetailFieldUiModel.CARD_NUMBER
    DetailBankCardFieldUiModel.CVV -> DetailFieldUiModel.CARD_CVV
    DetailBankCardFieldUiModel.EXPIRATION -> DetailFieldUiModel.CARD_EXPIRATION
    DetailBankCardFieldUiModel.PAYMENT_PIN -> DetailFieldUiModel.PAYMENT_PIN
}
