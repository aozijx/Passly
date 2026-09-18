package com.aozijx.passly.presentation.ui.vault.detail.model

import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel

data class DetailPresentationModel(
    val header: DetailHeaderUiModel,
    val content: DetailBodyUiModel,
    val overlays: DetailEditorOverlaysUiModel,
)

data class DetailBodyUiModel(
    val icon: DetailIconCardUiModel,
    val sections: Set<DetailSectionUiModel>,
    val credential: CredentialSectionUiState?,
    val otp: DetailOtpUiModel?,
    val bankCard: DetailBankCardUiModel?,
    val identity: DetailIdentityUiModel?,
    val wifi: DetailWifiUiModel?,
    val ssh: DetailSshUiModel?,
    val seedPhrase: DetailSeedPhraseUiModel?,
    val passkey: DetailPasskeyUiModel?,
    val relatedEntries: List<RelatedEntryUiModel>,
    val tags: Set<String>,
    val associations: DetailAssociatedInfoUiModel,
    val associatedApps: List<AppPackagePickerItemUiModel>,
    val packagePickerApps: List<AppPackagePickerItemUiModel>,
    val notes: DetailNotesUiModel,
    val metadata: DetailMetadataUiModel,
    val activities: List<DetailActivityUiModel>,
)

data class DetailSeedPhraseUiModel(
    val present: Boolean,
    val revealedValue: String?,
)

data class DetailPasskeyUiModel(
    val present: Boolean,
    val revealedValue: String?,
    val hardwareKeyInfo: String?,
)

data class DetailEditorOverlaysUiModel(
    val tagEditor: DetailTagEditorUiModel,
    val faviconEditor: DetailFaviconEditorUiModel,
    val savingTags: Boolean,
    val savingIcon: Boolean,
)

enum class DetailSectionUiModel {
    CREDENTIAL,
    OTP,
    BANK_CARD,
    IDENTITY,
    WIFI,
    SSH,
    SEED_PHRASE,
    PASSKEY,
}

enum class DetailFieldUiModel {
    USERNAME,
    PASSWORD,
    CARDHOLDER,
    CARD_NUMBER,
    CARD_CVV,
    PAYMENT_PIN,
    CARD_EXPIRATION,
    WIFI_SSID,
    SSH_PASSPHRASE,
    SSH_PRIVATE_KEY,
    SEED_PHRASE,
    PASSKEY_DATA,
    HARDWARE_INFO,
    ID_NUMBER,
}

sealed interface DetailContentEvent {
    data object EditIcon : DetailContentEvent
    data class StartFieldEdit(val field: DetailFieldUiModel, val initialValue: String) : DetailContentEvent
    data class CancelFieldEdit(val field: DetailFieldUiModel) : DetailContentEvent
    data class UpdateField(val field: DetailFieldUiModel, val value: String) : DetailContentEvent
    data class SaveField(val field: DetailFieldUiModel, val value: String) : DetailContentEvent
    data class ToggleFieldVisibility(val field: DetailFieldUiModel) : DetailContentEvent
    data class RevealFields(val fields: Set<DetailFieldUiModel>) : DetailContentEvent
    data class CopyField(val field: DetailFieldUiModel) : DetailContentEvent
    data object CopyOtpCode : DetailContentEvent
    data object ExportOtpQr : DetailContentEvent
    data class OpenRelatedEntry(val id: String) : DetailContentEvent
    data object OpenTagEditor : DetailContentEvent
    data object StartDomainEdit : DetailContentEvent
    data class UpdateDomain(val value: String) : DetailContentEvent
    data object SaveDomain : DetailContentEvent
    data object OpenPackagePicker : DetailContentEvent
    data class SelectAssociatedPackage(val packageName: String) : DetailContentEvent
    data object StartNotesEdit : DetailContentEvent
    data class UpdateNotes(val value: String) : DetailContentEvent
    data object SaveNotes : DetailContentEvent
}

sealed interface DetailEditorOverlayEvent {
    data class UpdateTagInput(val value: String) : DetailEditorOverlayEvent
    data class SubmitTag(val value: String) : DetailEditorOverlayEvent
    data class RemoveTag(val value: String) : DetailEditorOverlayEvent
    data object SaveTags : DetailEditorOverlayEvent
    data object DismissTagEditor : DetailEditorOverlayEvent
    data object ConfirmDiscardTags : DetailEditorOverlayEvent
    data object KeepEditingTags : DetailEditorOverlayEvent
    data class SelectFaviconTab(val tab: FaviconEditorTabUiModel) : DetailEditorOverlayEvent
    data class UpdateFaviconSearch(val value: String) : DetailEditorOverlayEvent
    data class SelectFaviconSource(val source: FaviconDraftSourceUiModel) : DetailEditorOverlayEvent
    data object UploadFavicon : DetailEditorOverlayEvent
    data class UpdateFaviconImageUrl(val value: String) : DetailEditorOverlayEvent
    data object DownloadFaviconImage : DetailEditorOverlayEvent
    data object SaveFavicon : DetailEditorOverlayEvent
    data object DismissFaviconEditor : DetailEditorOverlayEvent
    data object ConfirmDiscardFavicon : DetailEditorOverlayEvent
    data object KeepEditingFavicon : DetailEditorOverlayEvent
    data class CropFavicon(val zoom: Float, val offsetX: Float, val offsetY: Float) : DetailEditorOverlayEvent
    data object UseFaviconWithoutCrop : DetailEditorOverlayEvent
    data object CancelFaviconCrop : DetailEditorOverlayEvent
}
