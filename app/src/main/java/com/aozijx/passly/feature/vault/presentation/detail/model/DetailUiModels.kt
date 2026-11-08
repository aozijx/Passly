package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.feature.vault.presentation.detail.DetailField

data class DetailHeaderUiModel(
    val title: String,
    val favorite: Boolean,
    val editedTitle: String,
    val isEditingTitle: Boolean,
    val canEditFullForm: Boolean,
    val entryType: EntryType,
)

data class DetailMetadataUiModel(
    val createdAt: Long,
    val updatedAt: Long,
)

data class DetailActivityUiModel(
    val type: ActivityType,
    val context: EntryActivityContext,
    val createdAt: Long,
)

data class CredentialFieldUiState(
    @param:StringRes val titleRes: Int,
    val revealedValue: ScopedSensitiveText?,
    val isEditing: Boolean,
    val editedValue: String,
    val valueForEditing: String = editedValue,
)

data class CredentialSectionUiState(
    val username: CredentialFieldUiState?,
    val password: CredentialFieldUiState?,
)

data class RelatedEntryUiModel(
    val id: String,
    val title: String,
    val entryType: EntryType,
)

data class RelatedPasswordUiModel(
    val linkId: String,
    val targetEntryId: String,
    val title: String,
    val username: String?,
)

sealed interface DetailTagChipsUiState {
    data object Empty : DetailTagChipsUiState

    data class Items(val tags: List<String>) : DetailTagChipsUiState {
        init {
            require(tags.isNotEmpty())
        }
    }
}

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
    val cardholder: String?,
    val cardNumber: ProtectedDetailFieldUiState,
    val cvv: ProtectedDetailFieldUiState,
    val expiration: String?,
    val paymentPin: ProtectedDetailFieldUiState,
    val editingCardholder: Boolean,
    val editedCardholder: String,
    val editingCardNumber: Boolean,
    val editedCardNumber: String,
    val editingCvv: Boolean,
    val editedCvv: String,
    val bankDetails: List<DetailCardTextFieldUiModel> = emptyList(),
) {
    val hasCvv: Boolean get() = cvv.isPresent || editingCvv
    val canRevealMore: Boolean get() = cardNumber.isHidden || cvv.isHidden || paymentPin.isHidden
}

sealed interface ProtectedDetailFieldUiState {
    val isPresent: Boolean get() = this !is Absent
    val isHidden: Boolean get() = this is Hidden
    val isRevealed: Boolean get() = this is Revealed
    val revealedValue: String?
        get() = (this as? Revealed)?.value

    data object Absent : ProtectedDetailFieldUiState

    data object Hidden : ProtectedDetailFieldUiState

    data class Revealed(val value: String) : ProtectedDetailFieldUiState
}

data class DetailCardTextFieldUiModel(
    val field: DetailField,
    @get:StringRes @param:StringRes val labelRes: Int,
    val value: String?,
    val editing: Boolean,
    val editedValue: String,
)

data class DetailIdentityUiModel(
    val idNumber: ProtectedDetailFieldUiState,
    val username: String?,
)

data class DetailSshUiModel(
    val fingerprint: String,
    val passphrase: ProtectedDetailFieldUiState,
    val privateKey: ProtectedDetailFieldUiState,
    val isEditingPassphrase: Boolean,
    val editedPassphrase: String,
) {
    val canRevealMore: Boolean get() = passphrase.isHidden || privateKey.isHidden
}

sealed interface DetailOtpUiModel {
    data object Pending : DetailOtpUiModel

    data class Ready(
        val code: String,
        val progress: Float,
    ) : DetailOtpUiModel

    data object Failed : DetailOtpUiModel
}
