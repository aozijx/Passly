package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal object DetailSensitiveFieldKeyMapper {
    fun toDomain(key: String): SensitiveFieldKey? = when (key) {
        RevealedFieldKey.PASSWORD -> SensitiveFieldKey.PASSWORD
        RevealedFieldKey.CARD_NUMBER -> SensitiveFieldKey.CARD_NUMBER
        RevealedFieldKey.CVV -> SensitiveFieldKey.CARD_CVV
        RevealedFieldKey.PAYMENT_PIN -> SensitiveFieldKey.CARD_PAYMENT_PIN
        RevealedFieldKey.SSH_PRIVATE_KEY -> SensitiveFieldKey.SSH_PRIVATE_KEY
        RevealedFieldKey.SSH_PASSPHRASE -> SensitiveFieldKey.SSH_PASSPHRASE
        RevealedFieldKey.SEED_PHRASE -> SensitiveFieldKey.SEED_PHRASE
        RevealedFieldKey.PASSKEY_DATA -> SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE
        RevealedFieldKey.ID_NUMBER -> SensitiveFieldKey.IDENTITY_NUMBER
        RevealedFieldKey.RECOVERY_CODES -> SensitiveFieldKey.RECOVERY_CODES
        else -> null
    }

    fun toUi(key: SensitiveFieldKey): String? = when (key) {
        SensitiveFieldKey.PASSWORD -> RevealedFieldKey.PASSWORD
        SensitiveFieldKey.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
        SensitiveFieldKey.CARD_CVV -> RevealedFieldKey.CVV
        SensitiveFieldKey.CARD_PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
        SensitiveFieldKey.SSH_PRIVATE_KEY -> RevealedFieldKey.SSH_PRIVATE_KEY
        SensitiveFieldKey.SSH_PASSPHRASE -> RevealedFieldKey.SSH_PASSPHRASE
        SensitiveFieldKey.SEED_PHRASE -> RevealedFieldKey.SEED_PHRASE
        SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE -> RevealedFieldKey.PASSKEY_DATA
        SensitiveFieldKey.IDENTITY_NUMBER -> RevealedFieldKey.ID_NUMBER
        SensitiveFieldKey.RECOVERY_CODES -> RevealedFieldKey.RECOVERY_CODES
        SensitiveFieldKey.OTP_SECRET -> null
    }
}
