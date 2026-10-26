package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFieldUiModel

internal val DetailFieldUiModel.fieldKey: FieldKey
    get() = when (this) {
        DetailFieldUiModel.USERNAME -> FieldKey.USERNAME
        DetailFieldUiModel.PASSWORD -> FieldKey.PASSWORD
        DetailFieldUiModel.CARDHOLDER -> FieldKey.CARD_HOLDER
        DetailFieldUiModel.CARD_ISSUING_BANK -> FieldKey.CARD_ISSUING_BANK
        DetailFieldUiModel.CARD_BANK_BRANCH -> FieldKey.CARD_BANK_BRANCH
        DetailFieldUiModel.CARD_PRODUCT -> FieldKey.CARD_PRODUCT
        DetailFieldUiModel.CARD_CURRENCY -> FieldKey.CARD_CURRENCY
        DetailFieldUiModel.CARD_BANK_PHONE -> FieldKey.CARD_BANK_PHONE
        DetailFieldUiModel.BILLING_ADDRESS -> FieldKey.BILLING_ADDRESS
        DetailFieldUiModel.CARD_NUMBER -> FieldKey.CARD_NUMBER
        DetailFieldUiModel.CARD_CVV -> FieldKey.CARD_CVV
        DetailFieldUiModel.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        DetailFieldUiModel.CARD_EXPIRATION -> FieldKey.CARD_EXPIRATION
        DetailFieldUiModel.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        DetailFieldUiModel.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        DetailFieldUiModel.SEED_PHRASE -> FieldKey.SEED_PHRASE
        DetailFieldUiModel.PASSKEY_DATA -> FieldKey.PASSKEY_DATA
        DetailFieldUiModel.HARDWARE_INFO -> FieldKey.HARDWARE_INFO
        DetailFieldUiModel.ID_NUMBER -> FieldKey.ID_NUMBER
    }

internal val DetailFieldUiModel.draftKey: DetailDraftKey
    get() = DetailDraftKey.EntryField(fieldKey)
