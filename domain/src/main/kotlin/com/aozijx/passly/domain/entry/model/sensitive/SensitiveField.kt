package com.aozijx.passly.domain.entry.model.sensitive

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** A stable storage key for a separately encrypted sensitive field. */
enum class SensitiveFieldKey {
    PASSWORD,
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

/** Well-known field keys on the secret-field table that are not revealable sensitive values. */
object SecretFieldKeys {
    /** Aggregated low-sensitivity secret payload (structure, notes, custom fields). */
    const val STRUCT_BUNDLE = "STRUCT_BUNDLE"
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

/** A decrypted high-sensitivity value from one immutable revision snapshot. */
data class RevealedRevisionSensitiveField(
    val revisionId: String,
    val entryId: EntryId,
    val key: SensitiveFieldKey,
    val value: SensitiveValue,
)
