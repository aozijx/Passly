package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardEditorUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardProtectedValue
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailHardwareKeyInfoUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityNumberUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailIdentityUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyDataUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPasskeyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSeedPhraseUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshFingerprintUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPassphraseUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPrivateKeyUiState
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
    ): DetailProtectedValueProjection {
        if (sensitiveKey !in state.sensitiveFieldKeys) return DetailProtectedValueProjection.Absent
        return state
            .revealed(fieldKey)
            ?.copyToString()
            ?.let(DetailProtectedValueProjection::Revealed)
            ?: DetailProtectedValueProjection.Hidden
    }
}

internal sealed interface DetailProtectedValueProjection {
    data object Absent : DetailProtectedValueProjection

    data object Hidden : DetailProtectedValueProjection

    data class Revealed(
        val value: String,
    ) : DetailProtectedValueProjection
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

    fun editor(
        field: DetailField,
        isRevealed: Boolean = true,
    ): DetailBankCardEditorUiState =
        when (val edit = state.fieldEdits.edit(DetailDraftKey.EntryField(field.fieldKey))) {
            DetailFieldEdit.NotEditing -> {
                if (isRevealed) DetailBankCardEditorUiState.Available else DetailBankCardEditorUiState.ReadOnly
            }

            is DetailFieldEdit.Editing -> {
                DetailBankCardEditorUiState.Editing(edit.draft)
            }
        }
    val cardNumberEditor = editor(DetailField.CARD_NUMBER, cardNumber is DetailProtectedValueProjection.Revealed)
    val cvvEditor = editor(DetailField.CARD_CVV, cvv is DetailProtectedValueProjection.Revealed)

    fun addText(
        result: MutableList<DetailBankCardFieldUiModel>,
        field: DetailField,
        @StringRes labelRes: Int,
        value: String?,
    ) {
        result +=
            DetailBankCardFieldUiModel.Text(
                field = field,
                labelRes = labelRes,
                value = value.orEmpty(),
                editor = editor(field),
            )
    }

    fun protectedValue(value: DetailProtectedValueProjection): DetailBankCardProtectedValue =
        when (value) {
            DetailProtectedValueProjection.Absent, DetailProtectedValueProjection.Hidden -> DetailBankCardProtectedValue.Hidden
            is DetailProtectedValueProjection.Revealed -> DetailBankCardProtectedValue.Revealed(value.value)
        }

    val cardNumberEditing = cardNumberEditor is DetailBankCardEditorUiState.Editing
    val cardNumberHidden = cardNumber is DetailProtectedValueProjection.Hidden
    val cvvHidden = cvv is DetailProtectedValueProjection.Hidden
    val paymentPinHidden = paymentPin is DetailProtectedValueProjection.Hidden
    val cardFields =
        buildList {
            addText(this, DetailField.CARDHOLDER, R.string.cardholder, card?.cardHolder?.takeIf(String::isNotBlank))
            addText(this, DetailField.CARD_ISSUING_BANK, R.string.card_issuing_bank, card?.issuingBank)
            addText(this, DetailField.CARD_BANK_BRANCH, R.string.card_bank_branch, card?.bankBranch)
            addText(this, DetailField.CARD_PRODUCT, R.string.card_product, card?.cardProduct)
            addText(this, DetailField.CARD_CURRENCY, R.string.card_currency, card?.currency)
            addText(this, DetailField.CARD_BANK_PHONE, R.string.card_bank_phone, card?.bankPhone)
            addText(this, DetailField.BILLING_ADDRESS, R.string.billing_address, card?.billingAddress)
            if (cardNumber !is DetailProtectedValueProjection.Absent) {
                add(
                    DetailBankCardFieldUiModel.Protected(
                        field = DetailField.CARD_NUMBER,
                        labelRes = R.string.card_number,
                        value = protectedValue(cardNumber),
                        editor = cardNumberEditor,
                    ),
                )
            }
            if (cvv !is DetailProtectedValueProjection.Absent ||
                cvvEditor is DetailBankCardEditorUiState.Editing
            ) {
                add(
                    DetailBankCardFieldUiModel.Protected(
                        field = DetailField.CARD_CVV,
                        labelRes = R.string.card_cvv,
                        value = protectedValue(cvv),
                        editor = cvvEditor,
                        compactMask = true,
                    ),
                )
            }
            card?.cardExpiry?.let { expiry ->
                add(
                    DetailBankCardFieldUiModel.ReadOnly(
                        field = DetailField.CARD_EXPIRATION,
                        labelRes = R.string.card_expiration,
                        value = expiry,
                    ),
                )
            }
            if (paymentPin !is DetailProtectedValueProjection.Absent) {
                add(
                    DetailBankCardFieldUiModel.Protected(
                        field = DetailField.PAYMENT_PIN,
                        labelRes = R.string.payment_pin,
                        value = protectedValue(paymentPin),
                        editor = DetailBankCardEditorUiState.ReadOnly,
                    ),
                )
            }
        }

    return DetailBankCardUiModel(
        fields = cardFields,
        canRevealMore = !cardNumberEditing && (cardNumberHidden || cvvHidden || paymentPinHidden),
    )
}

internal fun detailIdentityUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailIdentityUiModel {
    val idNumber = fields.protected(FieldKey.ID_NUMBER, SensitiveFieldKey.IDENTITY_NUMBER)
    return DetailIdentityUiModel(
        idNumber =
            when (idNumber) {
                DetailProtectedValueProjection.Absent -> DetailIdentityNumberUiState.Absent
                DetailProtectedValueProjection.Hidden -> DetailIdentityNumberUiState.Hidden
                is DetailProtectedValueProjection.Revealed -> DetailIdentityNumberUiState.Revealed(idNumber.value)
            },
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
    val passphraseUiState =
        when (val edit = state.fieldEdits.edit(DetailDraftKey.EntryField(FieldKey.SSH_PASSPHRASE))) {
            is DetailFieldEdit.Editing -> {
                DetailSshPassphraseUiState.Editing(edit.draft)
            }

            DetailFieldEdit.NotEditing -> {
                when (passphrase) {
                    DetailProtectedValueProjection.Absent -> DetailSshPassphraseUiState.Absent
                    DetailProtectedValueProjection.Hidden -> DetailSshPassphraseUiState.Hidden
                    is DetailProtectedValueProjection.Revealed -> DetailSshPassphraseUiState.Revealed(passphrase.value)
                }
            }
        }
    val privateKeyUiState =
        when (privateKey) {
            DetailProtectedValueProjection.Absent -> DetailSshPrivateKeyUiState.Absent
            DetailProtectedValueProjection.Hidden -> DetailSshPrivateKeyUiState.Hidden
            is DetailProtectedValueProjection.Revealed -> DetailSshPrivateKeyUiState.Revealed(privateKey.value)
        }
    return DetailSshUiModel(
        fingerprint =
            entry.username
                .takeIf(String::isNotEmpty)
                ?.let(DetailSshFingerprintUiState::Available)
                ?: DetailSshFingerprintUiState.NotSet,
        passphrase = passphraseUiState,
        privateKey = privateKeyUiState,
        canRevealMore =
            passphrase is DetailProtectedValueProjection.Hidden ||
                privateKey is DetailProtectedValueProjection.Hidden,
    )
}

internal fun detailSeedPhraseUiModel(fields: DetailFieldProjection): DetailSeedPhraseUiModel =
    when (val protected = fields.protected(FieldKey.SEED_PHRASE, SensitiveFieldKey.SEED_PHRASE)) {
        DetailProtectedValueProjection.Absent -> {
            DetailSeedPhraseUiModel.Absent
        }

        DetailProtectedValueProjection.Hidden -> {
            DetailSeedPhraseUiModel.Hidden
        }

        is DetailProtectedValueProjection.Revealed -> {
            protected.value
                .split(" ")
                .filter(String::isNotBlank)
                .takeIf { words -> words.isNotEmpty() }
                ?.let(DetailSeedPhraseUiModel::RevealedWords)
                ?: DetailSeedPhraseUiModel.RevealedWithoutWords
        }
    }

internal fun detailPasskeyUiModel(
    entry: Entry,
    fields: DetailFieldProjection,
): DetailPasskeyUiModel =
    DetailPasskeyUiModel(
        data =
            when (
                val protected =
                    fields.protected(
                        FieldKey.PASSKEY_DATA,
                        SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE,
                    )
            ) {
                DetailProtectedValueProjection.Absent -> DetailPasskeyDataUiState.Absent
                DetailProtectedValueProjection.Hidden -> DetailPasskeyDataUiState.Hidden
                is DetailProtectedValueProjection.Revealed -> DetailPasskeyDataUiState.Revealed(protected.value)
            },
        hardwareKeyInfo =
            entry.secret.passkey
                ?.hardwareKeyInfo
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(DetailHardwareKeyInfoUiState::Available)
                ?: DetailHardwareKeyInfoUiState.NotSet,
    )

internal fun SensitiveValue.copyToString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
