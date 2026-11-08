package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey

internal val DetailField.fieldKey: FieldKey
    get() =
        when (this) {
            DetailField.USERNAME -> FieldKey.USERNAME
            DetailField.PASSWORD -> FieldKey.PASSWORD
            DetailField.CARDHOLDER -> FieldKey.CARD_HOLDER
            DetailField.CARD_ISSUING_BANK -> FieldKey.CARD_ISSUING_BANK
            DetailField.CARD_BANK_BRANCH -> FieldKey.CARD_BANK_BRANCH
            DetailField.CARD_PRODUCT -> FieldKey.CARD_PRODUCT
            DetailField.CARD_CURRENCY -> FieldKey.CARD_CURRENCY
            DetailField.CARD_BANK_PHONE -> FieldKey.CARD_BANK_PHONE
            DetailField.BILLING_ADDRESS -> FieldKey.BILLING_ADDRESS
            DetailField.CARD_NUMBER -> FieldKey.CARD_NUMBER
            DetailField.CARD_CVV -> FieldKey.CARD_CVV
            DetailField.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
            DetailField.CARD_EXPIRATION -> FieldKey.CARD_EXPIRATION
            DetailField.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
            DetailField.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
            DetailField.SEED_PHRASE -> FieldKey.SEED_PHRASE
            DetailField.PASSKEY_DATA -> FieldKey.PASSKEY_DATA
            DetailField.HARDWARE_INFO -> FieldKey.HARDWARE_INFO
            DetailField.ID_NUMBER -> FieldKey.ID_NUMBER
        }

internal val DetailField.draftKey: DetailDraftKey
    get() = DetailDraftKey.EntryField(fieldKey)
