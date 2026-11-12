package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.StringRes
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.DetailHistoryFilter

data class DetailHeaderUiModel(
    val title: DetailTitleUiState,
    val favorite: Boolean,
    val canEditFullForm: Boolean,
)

sealed interface DetailTitleUiState {
    data class Display(
        val text: String,
    ) : DetailTitleUiState

    data class Editing(
        val draft: String,
    ) : DetailTitleUiState
}

data class DetailMetadataUiModel(
    val createdAt: Long,
    val updatedAt: Long,
)

data class DetailHistoryItemUiModel(
    val id: String,
    val description: DetailHistoryDescriptionUiState,
    val icon: DetailHistoryIcon,
    val filter: DetailHistoryFilter,
    val affectedFields: DetailHistoryAffectedFieldsUiState,
    val createdAtMs: Long,
    val revision: DetailHistoryRevisionReference,
)

sealed interface DetailHistoryAffectedFieldsUiState {
    data object None : DetailHistoryAffectedFieldsUiState

    data class Labels(
        val items: List<DetailRevisionFieldLabelUiState>,
    ) : DetailHistoryAffectedFieldsUiState {
        init {
            require(items.isNotEmpty())
        }
    }
}

sealed interface DetailHistoryRevisionReference {
    data object Unlinked : DetailHistoryRevisionReference

    data class Pending(
        val version: Int,
    ) : DetailHistoryRevisionReference

    data class Linked(
        val version: Int,
        val revisionId: String,
    ) : DetailHistoryRevisionReference
}

sealed interface DetailHistoryDescriptionUiState {
    data class Label(
        @get:StringRes val labelRes: Int,
    ) : DetailHistoryDescriptionUiState

    data class Snapshot(
        val version: Int,
        @get:StringRes val changeLabelRes: Int,
    ) : DetailHistoryDescriptionUiState

    data class RevisionEvent(
        @get:StringRes val labelRes: Int,
        val version: Int,
    ) : DetailHistoryDescriptionUiState
}

enum class DetailHistoryIcon {
    CREATED,
    EDITED,
    VERSION_RESTORED,
    DELETED,
    TRASH_RESTORED,
    VIEWED,
    REVEALED,
    COPIED,
    AUTOFILLED,
    EXPORTED,
    IMPORTED,
    SNAPSHOT,
}

sealed interface CredentialFieldUiState {
    val field: DetailField

    @get:StringRes
    val titleRes: Int

    data class Hidden(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val titleRes: Int,
    ) : CredentialFieldUiState

    data class Revealed(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val titleRes: Int,
        val value: ScopedSensitiveText,
    ) : CredentialFieldUiState

    data class Editing(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val titleRes: Int,
        val draft: String,
        val showModifiedHint: Boolean,
    ) : CredentialFieldUiState
}

data class RelatedEntryUiModel(
    val id: String,
    val title: String,
    @field:StringRes val entryTypeLabelRes: Int,
)

data class RelatedPasswordUiModel(
    val linkId: String,
    val targetEntryId: String,
    val title: String,
    val username: String,
    val unlinkActionState: RelatedPasswordRowUnlinkUiState,
)

enum class RelatedPasswordRowUnlinkUiState { Available, Removing }

data class DetailAssociatedInfoUiModel(
    val domain: DetailAssociatedDomainUiState,
)

sealed interface DetailAssociatedDomainUiState {
    data class Display(
        val value: DetailAssociatedDomainValueUiState,
    ) : DetailAssociatedDomainUiState

    data class Editing(
        val draft: String,
    ) : DetailAssociatedDomainUiState
}

sealed interface DetailAssociatedDomainValueUiState {
    data object NotSet : DetailAssociatedDomainValueUiState

    data class Value(
        val text: String,
    ) : DetailAssociatedDomainValueUiState
}

sealed interface DetailNotesUiState {
    data object NotSet : DetailNotesUiState

    data class Markdown(
        val content: String,
    ) : DetailNotesUiState

    data class Editing(
        val draft: String,
    ) : DetailNotesUiState
}

data class DetailBankCardUiModel(
    val fields: List<DetailBankCardFieldUiModel>,
    val canRevealMore: Boolean,
)

sealed interface DetailBankCardFieldUiModel {
    val field: DetailField

    @get:StringRes
    val labelRes: Int

    data class Text(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val labelRes: Int,
        val value: String,
        val editor: DetailBankCardEditorUiState,
    ) : DetailBankCardFieldUiModel

    data class Protected(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val labelRes: Int,
        val value: DetailBankCardProtectedValue,
        val editor: DetailBankCardEditorUiState,
        val compactMask: Boolean = false,
    ) : DetailBankCardFieldUiModel {
        val displayText: String
            get() =
                when (value) {
                    DetailBankCardProtectedValue.Hidden -> ""
                    is DetailBankCardProtectedValue.Revealed -> value.value
                }

        val isRevealed: Boolean
            get() = value is DetailBankCardProtectedValue.Revealed
    }

    data class ReadOnly(
        override val field: DetailField,
        @get:StringRes @param:StringRes override val labelRes: Int,
        val value: String,
    ) : DetailBankCardFieldUiModel
}

sealed interface DetailBankCardEditorUiState {
    data object ReadOnly : DetailBankCardEditorUiState

    data object Available : DetailBankCardEditorUiState

    data class Editing(
        val draft: String,
    ) : DetailBankCardEditorUiState
}

sealed interface DetailBankCardProtectedValue {
    data object Hidden : DetailBankCardProtectedValue

    data class Revealed(
        val value: String,
    ) : DetailBankCardProtectedValue
}

sealed interface DetailIdentityNumberUiState {
    data object Absent : DetailIdentityNumberUiState

    data object Hidden : DetailIdentityNumberUiState

    data class Revealed(
        val value: String,
    ) : DetailIdentityNumberUiState
}

data class DetailIdentityUiModel(
    val idNumber: DetailIdentityNumberUiState,
    val username: String,
)

sealed interface DetailPasskeyDataUiState {
    data object Absent : DetailPasskeyDataUiState

    data object Hidden : DetailPasskeyDataUiState

    data class Revealed(
        val value: String,
    ) : DetailPasskeyDataUiState
}

sealed interface DetailHardwareKeyInfoUiState {
    data object NotSet : DetailHardwareKeyInfoUiState

    data class Available(
        val value: String,
    ) : DetailHardwareKeyInfoUiState
}

sealed interface DetailSshFingerprintUiState {
    data object NotSet : DetailSshFingerprintUiState

    data class Available(
        val value: String,
    ) : DetailSshFingerprintUiState
}

sealed interface DetailSshPassphraseUiState {
    data object Absent : DetailSshPassphraseUiState

    data object Hidden : DetailSshPassphraseUiState

    data class Revealed(
        val value: String,
    ) : DetailSshPassphraseUiState

    data class Editing(
        val draft: String,
    ) : DetailSshPassphraseUiState
}

sealed interface DetailSshPrivateKeyUiState {
    data object Absent : DetailSshPrivateKeyUiState

    data object Hidden : DetailSshPrivateKeyUiState

    data class Revealed(
        val value: String,
    ) : DetailSshPrivateKeyUiState {
        val preview: String get() = value.take(SSH_KEY_PREVIEW_LENGTH).plus("...")
    }
}

data class DetailSshUiModel(
    val fingerprint: DetailSshFingerprintUiState,
    val passphrase: DetailSshPassphraseUiState,
    val privateKey: DetailSshPrivateKeyUiState,
    val canRevealMore: Boolean,
)

private const val SSH_KEY_PREVIEW_LENGTH = 60
