package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailCardTextFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSeedPhraseUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel

/**
 * One projection of the storage facts used by every detail section:
 * metadata says whether a protected value exists; the reveal store may supply its plaintext.
 */
internal class DetailFieldProjection(
    private val state: DetailUiState,
) {
    fun protected(
        fieldKey: FieldKey,
        sensitiveKey: SensitiveFieldKey,
    ): ProtectedDetailFieldUiState {
        if (sensitiveKey !in state.sensitiveFieldKeys) return ProtectedDetailFieldUiState.Absent
        return state.revealed(fieldKey).copyToString()
            ?.let(ProtectedDetailFieldUiState::Revealed)
            ?: ProtectedDetailFieldUiState.Hidden
    }
}

internal fun detailBankCardUiModel(
    entry: Entry,
    state: DetailUiState,
    fields: DetailFieldProjection,
): DetailBankCardUiModel {
    val card = entry.secret.card
    val cardNumber = fields.protected(FieldKey.CARD_NUMBER, SensitiveFieldKey.CARD_NUMBER)
    val cvv = fields.protected(FieldKey.CARD_CVV, SensitiveFieldKey.CARD_CVV)
    val paymentPin = fields.protected(FieldKey.PAYMENT_PIN, SensitiveFieldKey.CARD_PAYMENT_PIN)

    fun bankDetail(
        field: DetailField,
        @StringRes labelRes: Int,
        value: String?,
    ) = DetailCardTextFieldUiModel(
        field = field,
        labelRes = labelRes,
        value = value,
        editing = state.fieldEdits.isEditing(field.fieldKey),
        editedValue = state.fieldEdits.draft(field.fieldKey),
    )

    return DetailBankCardUiModel(
        cardholder = card?.cardHolder?.takeIf(String::isNotBlank),
        cardNumber = cardNumber,
        cvv = cvv,
        expiration = card?.cardExpiry,
        paymentPin = paymentPin,
        editingCardholder = state.fieldEdits.isEditing(FieldKey.CARD_HOLDER),
        editedCardholder = state.fieldEdits.draft(FieldKey.CARD_HOLDER),
        editingCardNumber = state.fieldEdits.isEditing(FieldKey.CARD_NUMBER),
        editedCardNumber = state.fieldEdits.draft(FieldKey.CARD_NUMBER),
        editingCvv = state.fieldEdits.isEditing(FieldKey.CARD_CVV),
        editedCvv = state.fieldEdits.draft(FieldKey.CARD_CVV),
        bankDetails =
            listOf(
                bankDetail(DetailField.CARD_ISSUING_BANK, R.string.card_issuing_bank, card?.issuingBank),
                bankDetail(DetailField.CARD_BANK_BRANCH, R.string.card_bank_branch, card?.bankBranch),
                bankDetail(DetailField.CARD_PRODUCT, R.string.card_product, card?.cardProduct),
                bankDetail(DetailField.CARD_CURRENCY, R.string.card_currency, card?.currency),
                bankDetail(DetailField.CARD_BANK_PHONE, R.string.card_bank_phone, card?.bankPhone),
                bankDetail(DetailField.BILLING_ADDRESS, R.string.billing_address, card?.billingAddress),
            ),
    )
}

internal fun detailIdentityUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailIdentityUiModel {
    val idNumber = fields.protected(FieldKey.ID_NUMBER, SensitiveFieldKey.IDENTITY_NUMBER)
    return DetailIdentityUiModel(
        idNumber = idNumber,
        username = entry.username,
    )
}

internal fun detailSshUiModel(
    entry: Entry,
    state: DetailUiState,
    fields: DetailFieldProjection,
): DetailSshUiModel {
    val passphrase = fields.protected(FieldKey.SSH_PASSPHRASE, SensitiveFieldKey.SSH_PASSPHRASE)
    val privateKey = fields.protected(FieldKey.SSH_KEY, SensitiveFieldKey.SSH_PRIVATE_KEY)
    return DetailSshUiModel(
        fingerprint = entry.username,
        passphrase = passphrase,
        privateKey = privateKey,
        isEditingPassphrase = state.fieldEdits.isEditing(FieldKey.SSH_PASSPHRASE),
        editedPassphrase = state.fieldEdits.draft(FieldKey.SSH_PASSPHRASE),
    )
}

internal fun detailSeedPhraseUiModel(fields: DetailFieldProjection): DetailSeedPhraseUiModel =
    when (val protected = fields.protected(FieldKey.SEED_PHRASE, SensitiveFieldKey.SEED_PHRASE)) {
        ProtectedDetailFieldUiState.Absent -> DetailSeedPhraseUiModel.Absent
        ProtectedDetailFieldUiState.Hidden -> DetailSeedPhraseUiModel.Hidden
        is ProtectedDetailFieldUiState.Revealed ->
            protected.value
                .split(" ")
                .filter(String::isNotBlank)
                .takeIf { words -> words.isNotEmpty() }
                ?.let(DetailSeedPhraseUiModel::RevealedWords)
                ?: DetailSeedPhraseUiModel.RevealedWithoutWords
    }

internal fun detailPasskeyUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailPasskeyUiModel =
    DetailPasskeyUiModel(
        data =
            fields.protected(
            FieldKey.PASSKEY_DATA,
            SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE,
            ),
        hardwareKeyInfo = entry.secret.passkey?.hardwareKeyInfo?.takeIf(String::isNotBlank),
    )

internal fun DetailFieldEditState.isEditing(fieldKey: FieldKey): Boolean = isEditing(DetailDraftKey.EntryField(fieldKey))

internal fun DetailFieldEditState.draft(fieldKey: FieldKey): String = draft(DetailDraftKey.EntryField(fieldKey))

internal fun SensitiveValue?.copyToString(): String? {
    val source = this ?: return null
    val chars = source.toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
