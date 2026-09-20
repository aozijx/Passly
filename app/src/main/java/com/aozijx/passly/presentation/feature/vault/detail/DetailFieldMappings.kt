package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel

internal val DetailFieldUiModel.revealedKey: String?
    get() = when (this) {
        DetailFieldUiModel.USERNAME -> RevealedFieldKey.USERNAME
        DetailFieldUiModel.PASSWORD -> RevealedFieldKey.PASSWORD
        DetailFieldUiModel.CARDHOLDER -> RevealedFieldKey.CARDHOLDER
        DetailFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
        DetailFieldUiModel.CARD_CVV -> RevealedFieldKey.CVV
        DetailFieldUiModel.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
        DetailFieldUiModel.SSH_PASSPHRASE -> RevealedFieldKey.SSH_PASSPHRASE
        DetailFieldUiModel.SSH_PRIVATE_KEY -> RevealedFieldKey.SSH_PRIVATE_KEY
        DetailFieldUiModel.SEED_PHRASE -> RevealedFieldKey.SEED_PHRASE
        DetailFieldUiModel.PASSKEY_DATA -> RevealedFieldKey.PASSKEY_DATA
        DetailFieldUiModel.ID_NUMBER -> RevealedFieldKey.ID_NUMBER
        DetailFieldUiModel.CARD_EXPIRATION,
        DetailFieldUiModel.WIFI_SSID,
        DetailFieldUiModel.HARDWARE_INFO,
            -> null
    }

internal val DetailFieldUiModel.copyKey: FieldKey?
    get() = when (this) {
        DetailFieldUiModel.USERNAME -> FieldKey.USERNAME
        DetailFieldUiModel.PASSWORD -> FieldKey.PASSWORD
        DetailFieldUiModel.CARDHOLDER -> FieldKey.CARD_HOLDER
        DetailFieldUiModel.CARD_NUMBER -> FieldKey.CARD_NUMBER
        DetailFieldUiModel.CARD_CVV -> FieldKey.CARD_CVV
        DetailFieldUiModel.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        DetailFieldUiModel.CARD_EXPIRATION -> FieldKey.CARD_EXPIRATION
        DetailFieldUiModel.WIFI_SSID -> FieldKey.WIFI_SSID
        DetailFieldUiModel.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        DetailFieldUiModel.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        DetailFieldUiModel.SEED_PHRASE -> FieldKey.SEED_PHRASE
        DetailFieldUiModel.PASSKEY_DATA -> FieldKey.PASSKEY_DATA
        DetailFieldUiModel.HARDWARE_INFO -> FieldKey.HARDWARE_INFO
        DetailFieldUiModel.ID_NUMBER -> FieldKey.ID_NUMBER
    }
