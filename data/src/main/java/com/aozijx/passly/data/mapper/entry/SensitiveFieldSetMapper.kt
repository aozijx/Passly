package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

/**
 * Splits an [EntrySecret] into field-level values (stored as per-key ciphertext rows) and the
 * low-sensitivity aggregate (stored as the `STRUCT_BUNDLE` row), and merges them back.
 *
 * These are public because `app/feature/backup` (the documented data-integration exception)
 * needs the same split/merge for the portable backup format.
 */
fun EntrySecret.toSensitiveFieldValues(): Map<SensitiveFieldKey, String> = buildMap {
    fun putText(key: SensitiveFieldKey, value: String?) {
        value?.takeIf(String::isNotBlank)?.let { put(key, it) }
    }
    putText(SensitiveFieldKey.PASSWORD, login?.password)
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

/** The aggregate payload without any field-level value. Sensitive fields become `null`. */
fun EntrySecret.toBundleSecret(): EntrySecret {
    val credential = when (val value = credential) {
        is LoginCredential -> value.copy(password = null)
        is CardCredential -> value.copy(cardNumber = null, cardCvv = null, paymentPin = null)
        is IdentityCredential -> value.copy(idNumber = null, seedPhrase = null, recoveryCodes = emptyList())
        is SshCredential -> value.copy(privateKey = null, passphrase = null)
        is PasskeyCredential -> value.copy(privateKeyReference = null)
        is OtpCredential -> value.copy(config = value.config.copy(secret = null))
        else -> value
    }
    return copy(credential = credential)
}

/** Reassembles a complete secret from the bundle and decrypted field-level values. */
fun EntrySecret.mergeSensitiveFields(fields: Map<SensitiveFieldKey, String>): EntrySecret {
    val credential = when (val value = credential) {
        is LoginCredential -> fields[SensitiveFieldKey.PASSWORD]?.let { value.copy(password = it) } ?: value
        is CardCredential -> value.copy(
            cardNumber = fields[SensitiveFieldKey.CARD_NUMBER] ?: value.cardNumber,
            cardCvv = fields[SensitiveFieldKey.CARD_CVV] ?: value.cardCvv,
            paymentPin = fields[SensitiveFieldKey.CARD_PAYMENT_PIN] ?: value.paymentPin,
        )
        is IdentityCredential -> value.copy(
            idNumber = fields[SensitiveFieldKey.IDENTITY_NUMBER] ?: value.idNumber,
            seedPhrase = fields[SensitiveFieldKey.SEED_PHRASE] ?: value.seedPhrase,
            recoveryCodes = fields[SensitiveFieldKey.RECOVERY_CODES]?.lines() ?: value.recoveryCodes,
        )
        is SshCredential -> value.copy(
            privateKey = fields[SensitiveFieldKey.SSH_PRIVATE_KEY] ?: value.privateKey,
            passphrase = fields[SensitiveFieldKey.SSH_PASSPHRASE] ?: value.passphrase,
        )
        is PasskeyCredential -> fields[SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE]?.let {
            value.copy(privateKeyReference = it)
        } ?: value
        is OtpCredential -> fields[SensitiveFieldKey.OTP_SECRET]?.let {
            value.copy(config = value.config.copy(secret = it))
        } ?: value
        else -> value
    }
    return copy(credential = credential)
}

/**
 * Applies a UI-submitted secret on top of the currently stored one. A `null` field-level value
 * in `this` means "not loaded into the edit form", so the stored value is preserved; a non-null
 * value (including blank, which clears the field) is taken from `this`.
 */
internal fun EntrySecret.mergePreservedFields(current: EntrySecret): EntrySecret {
    val credential = when (val value = credential) {
        is LoginCredential -> if (value.password == null) {
            value.copy(password = current.login?.password)
        } else {
            value
        }

        is CardCredential -> value.copy(
            cardNumber = value.cardNumber ?: current.card?.cardNumber,
            cardCvv = value.cardCvv ?: current.card?.cardCvv,
            paymentPin = value.paymentPin ?: current.card?.paymentPin,
        )
        is IdentityCredential -> value.copy(
            idNumber = value.idNumber ?: current.identity?.idNumber,
            seedPhrase = value.seedPhrase ?: current.identity?.seedPhrase,
            recoveryCodes = value.recoveryCodes.ifEmpty { current.identity?.recoveryCodes.orEmpty() },
        )
        is SshCredential -> value.copy(
            privateKey = value.privateKey ?: current.ssh?.privateKey,
            passphrase = value.passphrase ?: current.ssh?.passphrase,
        )
        is PasskeyCredential -> if (value.privateKeyReference == null) {
            value.copy(privateKeyReference = current.passkey?.privateKeyReference)
        } else {
            value
        }

        is OtpCredential -> if (value.config.secret == null) {
            value.copy(config = value.config.copy(secret = current.otp?.config?.secret))
        } else {
            value
        }

        else -> value
    }
    return copy(credential = credential)
}
