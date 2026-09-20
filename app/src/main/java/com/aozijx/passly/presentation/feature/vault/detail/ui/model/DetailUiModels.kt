package com.aozijx.passly.presentation.feature.vault.detail.ui.model

data class DetailHeaderUiModel(
    val title: String,
    val favorite: Boolean,
    val editedTitle: String,
    val isEditingTitle: Boolean,
)

data class DetailContentUiModel(
    val relatedEntries: List<RelatedEntryUiModel>,
    val metadata: DetailMetadataUiModel,
    val activities: List<DetailActivityUiModel>,
)

data class DetailMetadataUiModel(val createdAt: Long, val updatedAt: Long)

data class DetailActivityUiModel(
    val type: DetailActivityTypeUiModel,
    val source: String?,
    val createdAt: Long,
)

enum class DetailActivityTypeUiModel {
    VIEW, COPY_USERNAME, COPY_PASSWORD, AUTOFILL, EXPORT, IMPORT,
    CREATE, UPDATE, SENSITIVE_CHANGE, DELETE, RESTORE,
}

enum class DetailEntryTypeUiModel {
    ACCOUNT, LOGIN, NOTE, BANK_CARD, ID_CARD, PASSPORT, DRIVER_LICENSE,
    SSH_KEY, WIFI, PASSKEY, OTP, DATABASE_CREDENTIAL, SERVER_CREDENTIAL,
    API_KEY, CRYPTO_WALLET, SEED_PHRASE, RECOVERY_CODE,
}

enum class CredentialFieldUiModel {
    USERNAME,
    PASSWORD,
}

data class CredentialFieldUiState(
    val visible: Boolean,
    val revealedValue: ScopedSensitiveText?,
    val isEditing: Boolean,
    val editedValue: String,
    val valueForEditing: String = editedValue,
)

data class CredentialSectionUiState(
    val username: CredentialFieldUiState,
    val password: CredentialFieldUiState,
)

interface CredentialSectionEventHandler {
    fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean)
    fun onValueChanged(field: CredentialFieldUiModel, value: String)
    fun onRevealRequested(field: CredentialFieldUiModel)
    fun onCopyRequested(field: CredentialFieldUiModel)
    fun onSaveRequested(field: CredentialFieldUiModel, value: String)
}

data class RelatedEntryUiModel(
    val id: String,
    val title: String,
    val entryType: DetailEntryTypeUiModel,
)

data class DetailAssociatedInfoUiModel(
    val domain: String?,
    val editedDomain: String,
    val isEditingDomain: Boolean,
)

data class DetailNotesUiModel(
    val notes: String?,
    val editedNotes: String,
    val isEditing: Boolean,
)

data class DetailBankCardUiModel(
    val cardholder: String?, val cardholderRevealed: Boolean,
    val cardNumber: String?, val cardNumberRevealed: Boolean, val hasCardNumber: Boolean,
    val cvv: String?, val cvvRevealed: Boolean, val hasCvv: Boolean,
    val expiration: String?,
    val paymentPin: String?, val paymentPinRevealed: Boolean, val hasPaymentPin: Boolean,
    val editingCardholder: Boolean, val editedCardholder: String,
    val editingCardNumber: Boolean, val editedCardNumber: String,
    val editingCvv: Boolean, val editedCvv: String,
    val canRevealMore: Boolean,
)

data class DetailIdentityUiModel(
    val hasIdNumber: Boolean,
    val idNumber: String?,
    val idNumberRevealed: Boolean,
    val username: String,
)

data class DetailWifiUiModel(
    val ssid: String, val password: String?, val passwordRevealed: Boolean,
    val isEditingPassword: Boolean, val editedPassword: String,
    val securityType: String, val isHidden: Boolean,
)

data class DetailSshUiModel(
    val fingerprint: String,
    val hasPassphrase: Boolean, val hasPrivateKey: Boolean,
    val passphrase: String?, val passphraseRevealed: Boolean,
    val privateKey: String?, val privateKeyRevealed: Boolean,
    val isEditingPassphrase: Boolean, val editedPassphrase: String,
    val canRevealMore: Boolean,
)

data class DetailOtpUiModel(
    val code: String?,
    val progress: Float,
    val isLoading: Boolean,
    val hasError: Boolean,
)
