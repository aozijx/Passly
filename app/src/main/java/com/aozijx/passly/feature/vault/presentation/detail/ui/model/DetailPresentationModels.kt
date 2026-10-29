package com.aozijx.passly.feature.vault.presentation.detail.ui.model

import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.history.RevisionFieldValue
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerItemUiModel

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
    val canShowWifiQr: Boolean,
    val bankCard: DetailBankCardUiModel?,
    val identity: DetailIdentityUiModel?,
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

data class DetailRevisionItemUiModel(
    val id: String,
    val version: Int,
    val createdAtMs: Long,
    val change: RevisionChange,
)

data class DetailRevisionDifferenceUiModel(
    val field: RevisionFieldId,
    val kind: RevisionDifferenceKind,
    val before: RevisionFieldValue?,
    val after: RevisionFieldValue?,
    val revealedValue: ScopedSensitiveText? = null,
)

sealed interface DetailRevisionSheetUiModel {
    data object Hidden : DetailRevisionSheetUiModel
    data class ListPage(val content: DetailRevisionListUiState) : DetailRevisionSheetUiModel
    data class ComparisonPage(
        val revisionId: String,
        val content: DetailRevisionComparisonUiState,
    ) : DetailRevisionSheetUiModel
}

sealed interface DetailRevisionListUiState {
    data object Loading : DetailRevisionListUiState
    data class Ready(val revisions: List<DetailRevisionItemUiModel>) : DetailRevisionListUiState
    data object Failed : DetailRevisionListUiState
}

sealed interface DetailRevisionComparisonUiState {
    data object Loading : DetailRevisionComparisonUiState
    data class Ready(
        val differences: List<DetailRevisionDifferenceUiModel>,
        val restore: DetailRevisionRestoreUiState,
    ) : DetailRevisionComparisonUiState
    data object Failed : DetailRevisionComparisonUiState
}

enum class DetailRevisionRestoreUiState {
    Idle,
    Confirming,
    Restoring,
}

enum class DetailSectionUiModel {
    CREDENTIAL,
    OTP,
    BANK_CARD,
    IDENTITY,
    SSH,
    SEED_PHRASE,
    PASSKEY,
}

enum class DetailFieldUiModel {
    USERNAME,
    PASSWORD,
    CARDHOLDER,
    CARD_ISSUING_BANK,
    CARD_BANK_BRANCH,
    CARD_PRODUCT,
    CARD_CURRENCY,
    CARD_BANK_PHONE,
    BILLING_ADDRESS,
    CARD_NUMBER,
    CARD_CVV,
    PAYMENT_PIN,
    CARD_EXPIRATION,
    SSH_PASSPHRASE,
    SSH_PRIVATE_KEY,
    SEED_PHRASE,
    PASSKEY_DATA,
    HARDWARE_INFO,
    ID_NUMBER,
}
