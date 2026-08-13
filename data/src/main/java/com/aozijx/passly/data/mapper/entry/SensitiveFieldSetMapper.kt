package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun EntrySecret.toSensitiveFieldValues(): Map<SensitiveFieldKey, String> = buildMap {
    fun putText(key: SensitiveFieldKey, value: String?) {
        value?.takeIf(String::isNotBlank)?.let { put(key, it) }
    }
    putText(SensitiveFieldKey.CARD_NUMBER, card?.cardNumber)
    putText(SensitiveFieldKey.CARD_CVV, card?.cardCvv)
    putText(SensitiveFieldKey.CARD_PAYMENT_PIN, card?.paymentPin)
    putText(SensitiveFieldKey.IDENTITY_NUMBER, identity?.idNumber)
    putText(SensitiveFieldKey.SEED_PHRASE, identity?.seedPhrase)
    identity?.recoveryCodes?.takeIf(List<String>::isNotEmpty)?.let {
        put(SensitiveFieldKey.RECOVERY_CODES, it.joinToString("\n"))
    }
    putText(SensitiveFieldKey.SSH_PRIVATE_KEY, ssh?.privateKey)
    putText(SensitiveFieldKey.SSH_PASSPHRASE, ssh?.passphrase)
    putText(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE, passkey?.privateKeyReference)
    putText(SensitiveFieldKey.OTP_SECRET, otp?.config?.secret)
}
