package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailIdentityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSeedPhraseUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSshUiModel

/**
 * One projection of the storage facts used by every detail section:
 * metadata says whether a protected value exists; the reveal store may supply its plaintext.
 */
internal class DetailFieldProjection(private val state: DetailUiState) {
    fun protected(fieldKey: FieldKey, sensitiveKey: SensitiveFieldKey): DetailProtectedField =
        DetailProtectedField(
            present = sensitiveKey in state.sensitiveFieldKeys,
            value = state.revealed(fieldKey).copyToString(),
        )
}

internal data class DetailProtectedField(
    val present: Boolean,
    val value: String?,
) {
    val revealed: Boolean get() = value != null
    val hidden: Boolean get() = present && !revealed
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

    return DetailBankCardUiModel(
        cardholder = card?.cardHolder?.takeIf(String::isNotBlank) ?: entry.username,
        cardholderRevealed = true,
        cardNumber = cardNumber.value,
        cardNumberRevealed = cardNumber.revealed,
        hasCardNumber = cardNumber.present,
        cvv = cvv.value,
        cvvRevealed = cvv.revealed,
        hasCvv = cvv.present || state.fieldEdits.isEditing(FieldKey.CARD_CVV),
        expiration = card?.cardExpiry,
        paymentPin = paymentPin.value,
        paymentPinRevealed = paymentPin.revealed,
        hasPaymentPin = paymentPin.present,
        editingCardholder = state.fieldEdits.isEditing(FieldKey.CARD_HOLDER),
        editedCardholder = state.fieldEdits.draft(FieldKey.CARD_HOLDER),
        editingCardNumber = state.fieldEdits.isEditing(FieldKey.CARD_NUMBER),
        editedCardNumber = state.fieldEdits.draft(FieldKey.CARD_NUMBER),
        editingCvv = state.fieldEdits.isEditing(FieldKey.CARD_CVV),
        editedCvv = state.fieldEdits.draft(FieldKey.CARD_CVV),
        canRevealMore = cardNumber.hidden || cvv.hidden || paymentPin.hidden,
    )
}

internal fun detailIdentityUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailIdentityUiModel {
    val idNumber = fields.protected(FieldKey.ID_NUMBER, SensitiveFieldKey.IDENTITY_NUMBER)
    return DetailIdentityUiModel(idNumber.present, idNumber.value, idNumber.revealed, entry.username)
}

internal fun detailSshUiModel(
    entry: Entry,
    state: DetailUiState,
    fields: DetailFieldProjection,
): DetailSshUiModel {
    val passphrase = fields.protected(FieldKey.SSH_PASSPHRASE, SensitiveFieldKey.SSH_PASSPHRASE)
    val privateKey = fields.protected(FieldKey.SSH_KEY, SensitiveFieldKey.SSH_PRIVATE_KEY)
    return DetailSshUiModel(
        entry.username,
        passphrase.present,
        privateKey.present,
        passphrase.value,
        passphrase.revealed,
        privateKey.value,
        privateKey.revealed,
        state.fieldEdits.isEditing(FieldKey.SSH_PASSPHRASE),
        state.fieldEdits.draft(FieldKey.SSH_PASSPHRASE),
        privateKey.hidden || passphrase.hidden,
    )
}

internal fun detailSeedPhraseUiModel(fields: DetailFieldProjection): DetailSeedPhraseUiModel =
    fields.protected(FieldKey.SEED_PHRASE, SensitiveFieldKey.SEED_PHRASE).let {
        DetailSeedPhraseUiModel(present = it.present, revealedValue = it.value)
    }

internal fun detailPasskeyUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailPasskeyUiModel = fields.protected(
    FieldKey.PASSKEY_DATA,
    SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE,
).let {
    DetailPasskeyUiModel(
        present = it.present,
        revealedValue = it.value,
        hardwareKeyInfo = entry.secret.passkey?.hardwareKeyInfo,
    )
}

internal fun DetailFieldEditState.isEditing(fieldKey: FieldKey): Boolean =
    isEditing(DetailDraftKey.EntryField(fieldKey))

internal fun DetailFieldEditState.draft(fieldKey: FieldKey): String =
    draft(DetailDraftKey.EntryField(fieldKey))

internal fun SensitiveValue?.copyToString(): String? {
    val source = this ?: return null
    val chars = source.toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
