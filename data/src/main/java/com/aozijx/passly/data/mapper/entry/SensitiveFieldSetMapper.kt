package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal sealed interface SensitiveFieldUpdate {
    data object Preserve : SensitiveFieldUpdate
    data object Clear : SensitiveFieldUpdate
    data class Replace(val value: String) : SensitiveFieldUpdate
}

/** Detects effective writes without decrypting untouched field-level rows. */
internal fun EntrySecret.hasStoredChanges(
    storedBundle: EntrySecret,
    storedKeys: Set<SensitiveFieldKey>,
): Boolean = toBundleSecret() != storedBundle.toBundleSecret() ||
    toSensitiveFieldUpdates().any { (key, update) ->
        when (update) {
            SensitiveFieldUpdate.Preserve -> false
            SensitiveFieldUpdate.Clear -> key in storedKeys
            is SensitiveFieldUpdate.Replace -> true
        }
    }

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

/**
 * Describes the high-sensitivity rows explicitly submitted by an entry edit.
 *
 * A missing value means the editor never loaded that secret and must preserve the stored row;
 * a blank value is an intentional clear. This lets ordinary updates avoid decrypting every
 * sensitive row just to write the low-sensitivity bundle.
 */
internal fun EntrySecret.toSensitiveFieldUpdates(): Map<SensitiveFieldKey, SensitiveFieldUpdate> =
    buildMap {
        fun putText(key: SensitiveFieldKey, value: String?) {
            put(
                key,
                when {
                    value == null -> SensitiveFieldUpdate.Preserve
                    value.isBlank() -> SensitiveFieldUpdate.Clear
                    else -> SensitiveFieldUpdate.Replace(value)
                },
            )
        }

        when (val value = credential) {
            is LoginCredential -> putText(SensitiveFieldKey.PASSWORD, value.password)
            is CardCredential -> {
                putText(SensitiveFieldKey.CARD_NUMBER, value.cardNumber)
                putText(SensitiveFieldKey.CARD_CVV, value.cardCvv)
                putText(SensitiveFieldKey.CARD_PAYMENT_PIN, value.paymentPin)
            }
            is IdentityCredential -> {
                putText(SensitiveFieldKey.IDENTITY_NUMBER, value.idNumber)
                putText(SensitiveFieldKey.SEED_PHRASE, value.seedPhrase)
                putText(
                    SensitiveFieldKey.RECOVERY_CODES,
                    value.recoveryCodes.takeIf(List<String>::isNotEmpty)?.joinToString("\n"),
                )
            }
            is SshCredential -> {
                putText(SensitiveFieldKey.SSH_PRIVATE_KEY, value.privateKey)
                putText(SensitiveFieldKey.SSH_PASSPHRASE, value.passphrase)
            }
            is PasskeyCredential -> putText(
                SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE,
                value.privateKeyReference,
            )
            is OtpCredential -> putText(SensitiveFieldKey.OTP_SECRET, value.config.secret)
            else -> Unit
        }
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
