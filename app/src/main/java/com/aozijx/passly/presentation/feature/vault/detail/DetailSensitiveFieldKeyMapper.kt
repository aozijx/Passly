package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey

internal object DetailSensitiveFieldKeyMapper {
    fun toFieldKey(key: String): FieldKey? = when (key) {
        RevealedFieldKey.USERNAME -> FieldKey.USERNAME
        RevealedFieldKey.PASSWORD -> FieldKey.PASSWORD
        RevealedFieldKey.CARDHOLDER -> FieldKey.CARD_HOLDER
        RevealedFieldKey.CARD_NUMBER -> FieldKey.CARD_NUMBER
        RevealedFieldKey.CVV -> FieldKey.CARD_CVV
        RevealedFieldKey.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
        RevealedFieldKey.SSH_PRIVATE_KEY -> FieldKey.SSH_KEY
        RevealedFieldKey.SSH_PASSPHRASE -> FieldKey.SSH_PASSPHRASE
        RevealedFieldKey.SEED_PHRASE -> FieldKey.SEED_PHRASE
        RevealedFieldKey.PASSKEY_DATA -> FieldKey.PASSKEY_DATA
        RevealedFieldKey.ID_NUMBER -> FieldKey.ID_NUMBER
        RevealedFieldKey.RECOVERY_CODES -> FieldKey.RECOVERY_CODES
        else -> null
    }

    fun toUiKey(key: FieldKey): String? = when (key) {
        FieldKey.USERNAME -> RevealedFieldKey.USERNAME
        FieldKey.PASSWORD -> RevealedFieldKey.PASSWORD
        FieldKey.CARD_HOLDER -> RevealedFieldKey.CARDHOLDER
        FieldKey.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
        FieldKey.CARD_CVV -> RevealedFieldKey.CVV
        FieldKey.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
        FieldKey.SSH_KEY -> RevealedFieldKey.SSH_PRIVATE_KEY
        FieldKey.SSH_PASSPHRASE -> RevealedFieldKey.SSH_PASSPHRASE
        FieldKey.SEED_PHRASE -> RevealedFieldKey.SEED_PHRASE
        FieldKey.PASSKEY_DATA -> RevealedFieldKey.PASSKEY_DATA
        FieldKey.ID_NUMBER -> RevealedFieldKey.ID_NUMBER
        FieldKey.RECOVERY_CODES -> RevealedFieldKey.RECOVERY_CODES
        else -> null
    }
}