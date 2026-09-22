package com.aozijx.passly.presentation.feature.vault.detail.ui.model

import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailBankCardFieldUiModel

interface DetailContentCallbacks {
    fun onIconEditRequested()
    fun onCredentialEditingChanged(
        field: CredentialFieldUiModel,
        editing: Boolean,
        initialValue: String,
    )
    fun onCredentialValueChanged(field: CredentialFieldUiModel, value: String)
    fun onCredentialSaveRequested(field: CredentialFieldUiModel, value: String)
    fun onCredentialCopyRequested(field: CredentialFieldUiModel)
    fun onCredentialRevealRequested(field: CredentialFieldUiModel)
    fun onFieldEditStarted(field: DetailFieldUiModel, initialValue: String)
    fun onFieldValueChanged(field: DetailFieldUiModel, value: String)
    fun onFieldSaveRequested(field: DetailFieldUiModel, value: String)
    fun onFieldCopyRequested(field: DetailFieldUiModel)
    fun onFieldRevealRequested(field: DetailFieldUiModel)
    fun onBankCardValueChanged(field: DetailBankCardFieldUiModel, value: String)
    fun onBankCardEditStarted(field: DetailBankCardFieldUiModel, value: String)
    fun onBankCardSaveRequested(field: DetailBankCardFieldUiModel, value: String)
    fun onBankCardCopyRequested(field: DetailBankCardFieldUiModel)
    fun onBankCardRevealRequested(field: DetailBankCardFieldUiModel)
    fun onBankCardRevealAllRequested()
    fun onSshRevealAllRequested()
    fun onOtpQrRequested()
    fun onOtpCopyRequested()
    fun onOtpQrDismissed()
    fun onRelatedEntryRequested(entryId: String)
    fun onTagEditorRequested()
    fun onDomainEditStarted()
    fun onDomainChanged(value: String)
    fun onDomainSaveRequested(value: String)
    fun onPackagePickerRequested()
    fun onPackageSelected(packageName: String)
    fun onNotesEditStarted()
    fun onNotesChanged(value: String)
    fun onNotesSaveRequested(value: String)
}

interface DetailEditorOverlayCallbacks {
    fun onTagInputChanged(value: String)
    fun onTagSubmitted(value: String)
    fun onTagRemoved(value: String)
    fun onTagsSaveRequested()
    fun onTagEditorDismissed()
    fun onTagDiscardConfirmed()
    fun onTagEditingContinued()
    fun onFaviconTabSelected(tab: FaviconEditorTabUiModel)
    fun onFaviconSearchChanged(value: String)
    fun onFaviconSourceSelected(source: FaviconDraftSourceUiModel)
    fun onFaviconUploadRequested()
    fun onFaviconImageUrlChanged(value: String)
    fun onFaviconDownloadRequested()
    fun onFaviconSaveRequested()
    fun onFaviconEditorDismissed()
    fun onFaviconDiscardConfirmed()
    fun onFaviconEditingContinued()
    fun onFaviconCropRequested(zoom: Float, offsetX: Float, offsetY: Float)
    fun onFaviconUseWithoutCropRequested()
    fun onFaviconCropCancelled()
}
