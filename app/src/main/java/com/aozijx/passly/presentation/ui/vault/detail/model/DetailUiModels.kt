package com.aozijx.passly.presentation.ui.vault.detail.model

data class DetailScreenUiModel(
    val entryId: String,
    val title: String,
    val username: String,
    val entryType: DetailEntryTypeUiModel,
    val favorite: Boolean,
    val iconName: String?,
    val iconCustomPath: String?,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val editedTitle: String,
    val isEditingTitle: Boolean,
    val validationError: String?,
    val isAccessHistoryEnabled: Boolean,
    val isFaviconDownloading: Boolean,
    val sections: List<DetailSectionUiModel>,
    val relatedEntries: List<RelatedEntryUiModel>,
    val associatedInfo: DetailAssociatedInfoUiModel,
    val notes: DetailNotesUiModel,
    val metadata: DetailMetadataUiModel,
    val activities: List<DetailActivityUiModel>,
    val otp: DetailOtpUiModel?,
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

data class DetailRevisionUiModel(val id: String, val version: Long, val createdAt: Long)

enum class DetailEntryTypeUiModel {
    ACCOUNT, LOGIN, NOTE, BANK_CARD, ID_CARD, PASSPORT, DRIVER_LICENSE,
    SSH_KEY, WIFI, PASSKEY, OTP, DATABASE_CREDENTIAL, SERVER_CREDENTIAL,
    API_KEY, CRYPTO_WALLET, SEED_PHRASE, RECOVERY_CODE,
}

data class DetailSectionUiModel(
    val kind: DetailSectionKindUiModel,
    val fields: List<DetailFieldUiModel> = emptyList(),
)

enum class DetailSectionKindUiModel {
    CREDENTIAL, OTP, BANK_CARD, IDENTITY, WIFI, SSH, SEED_PHRASE, PASSKEY,
    ENTRY_TYPE, ASSOCIATED_INFO, NOTES, METADATA, ACTIVITY,
}

data class DetailFieldUiModel(
    val key: String,
    val text: String? = null,
    val sensitiveText: ScopedSensitiveText = ScopedSensitiveText.Empty,
    val isRevealed: Boolean = false,
    val isEditing: Boolean = false,
)

data class RelatedEntryUiModel(
    val id: String,
    val title: String,
    val entryType: DetailEntryTypeUiModel,
)

data class DetailAssociatedInfoUiModel(
    val domain: String?,
    val applicationIds: List<String>,
    val isEditingDomain: Boolean,
    val isFaviconDownloading: Boolean,
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

interface DetailUiEventHandler {
    fun onBack()
    fun onInteraction()
    fun onTitleEditStarted()
    fun onTitleChanged(value: String)
    fun onTitleEditCancelled()
    fun onTitleSaved()
    fun onFavoriteToggled()
    fun onRevealRequested(fieldKey: String)
    fun onCopyRequested(fieldKey: String)
    fun onFieldSaved(fieldKey: String, value: String)
    fun onRelatedEntryOpened(entryId: String)

    data object None : DetailUiEventHandler {
        override fun onBack() = Unit
        override fun onInteraction() = Unit
        override fun onTitleEditStarted() = Unit
        override fun onTitleChanged(value: String) = Unit
        override fun onTitleEditCancelled() = Unit
        override fun onTitleSaved() = Unit
        override fun onFavoriteToggled() = Unit
        override fun onRevealRequested(fieldKey: String) = Unit
        override fun onCopyRequested(fieldKey: String) = Unit
        override fun onFieldSaved(fieldKey: String, value: String) = Unit
        override fun onRelatedEntryOpened(entryId: String) = Unit
    }
}
