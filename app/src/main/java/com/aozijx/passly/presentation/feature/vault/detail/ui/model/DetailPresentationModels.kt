package com.aozijx.passly.presentation.feature.vault.detail.ui.model

import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerItemUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailBankCardFieldUiModel

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
