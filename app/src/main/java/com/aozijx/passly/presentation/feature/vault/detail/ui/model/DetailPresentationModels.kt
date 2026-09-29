package com.aozijx.passly.presentation.feature.vault.detail.ui.model

import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.presentation.shared.components.AppPackagePickerItemUiModel

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
    val revisionHistory: DetailRevisionSheetUiModel,
)

enum class DetailRevisionDestinationUiModel { LIST, COMPARISON }

data class DetailRevisionItemUiModel(
    val id: String,
    val version: Int,
    val createdAtMs: Long,
    val change: RevisionChange,
)

data class DetailRevisionDifferenceUiModel(
    val field: RevisionFieldId,
    val kind: RevisionDifferenceKind,
    val before: String?,
    val after: String?,
    val revealedValue: ScopedSensitiveText? = null,
)

data class DetailRevisionSheetUiModel(
    val visible: Boolean = false,
    val destination: DetailRevisionDestinationUiModel = DetailRevisionDestinationUiModel.LIST,
    val revisions: List<DetailRevisionItemUiModel> = emptyList(),
    val differences: List<DetailRevisionDifferenceUiModel> = emptyList(),
    val loading: Boolean = false,
    val restoring: Boolean = false,
    val confirmRestore: Boolean = false,
    val failure: String? = null,
    val selectedRevisionId: String? = null,
    val revealableKeys: Set<SensitiveFieldKey> = emptySet(),
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
