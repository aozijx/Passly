package com.aozijx.passly.feature.vault.presentation.detail.ui.model

import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType

data class DetailHeaderUiModel(
    val title: String,
    val favorite: Boolean,
    val editedTitle: String,
    val isEditingTitle: Boolean,
    val canEditFullForm: Boolean,
    val entryType: EntryType,
)

data class DetailContentUiModel(
    val relatedEntries: List<RelatedEntryUiModel>,
    val metadata: DetailMetadataUiModel,
    val activities: List<DetailActivityUiModel>,
)

data class DetailMetadataUiModel(val createdAt: Long, val updatedAt: Long)

data class DetailActivityUiModel(
    val type: ActivityType,
    val context: EntryActivityContext,
    val createdAt: Long,
)

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

data class RelatedEntryUiModel(
    val id: String,
    val title: String,
    val entryType: EntryType,
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
    val bankDetails: List<DetailCardTextFieldUiModel> = emptyList(),
)

data class DetailCardTextFieldUiModel(
    val field: DetailFieldUiModel,
    @get:StringRes @param:StringRes val labelRes: Int,
    val value: String?,
    val editing: Boolean,
    val editedValue: String,
)

data class DetailIdentityUiModel(
    val hasIdNumber: Boolean,
    val idNumber: String?,
    val idNumberRevealed: Boolean,
    val username: String,
)

data class DetailSshUiModel(
    val fingerprint: String,
    val hasPassphrase: Boolean, val hasPrivateKey: Boolean,
    val passphrase: String?, val passphraseRevealed: Boolean,
    val privateKey: String?, val privateKeyRevealed: Boolean,
    val isEditingPassphrase: Boolean, val editedPassphrase: String,
    val canRevealMore: Boolean,
)

sealed interface DetailOtpUiModel {
    data object Pending : DetailOtpUiModel
    data class Ready(val code: String, val progress: Float) : DetailOtpUiModel
    data object Failed : DetailOtpUiModel
}
