package com.aozijx.passly.domain.entry.model.sensitive

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** A stable storage key for a separately encrypted high-sensitivity field. */
enum class SensitiveFieldKey {
    CARD_NUMBER,
    CARD_CVV,
    CARD_PAYMENT_PIN,
    IDENTITY_NUMBER,
    SEED_PHRASE,
    RECOVERY_CODES,
    SSH_PRIVATE_KEY,
    SSH_PASSPHRASE,
    PASSKEY_PRIVATE_REFERENCE,
    OTP_SECRET
}

/** Non-secret metadata used to render whether a protected value exists. */
data class SensitiveFieldPresence(
    val entryId: EntryId,
    val keys: Set<SensitiveFieldKey>
) {
    operator fun contains(key: SensitiveFieldKey): Boolean = key in keys
}

/** A single decrypted value. Callers must wipe [value] as soon as it leaves the screen. */
data class RevealedSensitiveField(
    val entryId: EntryId,
    val key: SensitiveFieldKey,
    val value: SensitiveValue
)
