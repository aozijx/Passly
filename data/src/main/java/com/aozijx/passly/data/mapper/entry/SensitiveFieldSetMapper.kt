package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.withSecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal sealed interface SensitiveFieldUpdate {
    data object Preserve : SensitiveFieldUpdate

    data object Clear : SensitiveFieldUpdate

    data class Replace(
        val value: String,
    ) : SensitiveFieldUpdate
}

/** Detects effective writes without decrypting untouched field-level rows. */
internal fun EntrySecret.hasStoredChanges(
    storedBundle: EntrySecret,
    storedKeys: Set<SensitiveFieldKey>,
): Boolean =
    toBundleSecret() != storedBundle.toBundleSecret() ||
        toSensitiveFieldUpdates().any { (key, update) ->
            when (update) {
                SensitiveFieldUpdate.Preserve -> false
                SensitiveFieldUpdate.Clear -> key in storedKeys
                is SensitiveFieldUpdate.Replace -> true
            }
        }

/**
 * Projects each credential's field-level values once for extraction and edit classification.
 * Backup calls the owning module's `SecretFieldStore` boundary rather than this mapper; this mapping is
 * private to Data's persistence and revision-snapshot implementation.
 */
private fun EntryCredential.sensitiveFieldProjection(): Map<SensitiveFieldKey, String?> =
    when (this) {
        EntryCredential.None, is WifiCredential -> {
            emptyMap()
        }

        is LoginCredential -> {
            mapOf(SensitiveFieldKey.PASSWORD to password)
        }

        is CardCredential -> {
            mapOf(
                SensitiveFieldKey.CARD_NUMBER to cardNumber,
                SensitiveFieldKey.CARD_CVV to cardCvv,
                SensitiveFieldKey.CARD_PAYMENT_PIN to paymentPin,
            )
        }

        is IdentityCredential -> {
            mapOf(
                SensitiveFieldKey.IDENTITY_NUMBER to idNumber,
                SensitiveFieldKey.SEED_PHRASE to seedPhrase,
                SensitiveFieldKey.RECOVERY_CODES to recoveryCodes.takeIf(List<String>::isNotEmpty)?.joinToString("\n"),
            )
        }

        is SshCredential -> {
            mapOf(
                SensitiveFieldKey.SSH_PRIVATE_KEY to privateKey,
                SensitiveFieldKey.SSH_PASSPHRASE to passphrase,
            )
        }

        is PasskeyCredential -> {
            mapOf(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE to privateKeyReference)
        }

        is OtpCredential -> {
            mapOf(SensitiveFieldKey.OTP_SECRET to config.secret)
        }
    }

/**
 * Splits an [EntrySecret] into field-level values (stored as per-key ciphertext rows) and the
 * low-sensitivity aggregate (stored as the `STRUCT_BUNDLE` row).
 */
internal fun EntrySecret.toSensitiveFieldValues(): Map<SensitiveFieldKey, String> =
    buildMap {
        credential.sensitiveFieldProjection().forEach { (key, value) ->
            // Recovery-code replacement preserves every non-empty list, including blank entries.
            value?.takeIf { key == SensitiveFieldKey.RECOVERY_CODES || it.isNotBlank() }?.let { put(key, it) }
        }
    }

/**
 * Describes the high-sensitivity rows explicitly submitted by an entry edit.
 *
 * A missing value means the editor never loaded that secret and must preserve the stored row;
 * a blank value is an intentional clear. This lets ordinary updates avoid decrypting every
 * sensitive row just to write the low-sensitivity bundle.
 */
internal fun EntrySecret.toSensitiveFieldUpdates(): Map<SensitiveFieldKey, SensitiveFieldUpdate> =
    credential.sensitiveFieldProjection().mapValues { (_, value) ->
        when {
            value == null -> SensitiveFieldUpdate.Preserve
            value.isBlank() -> SensitiveFieldUpdate.Clear
            else -> SensitiveFieldUpdate.Replace(value)
        }
    }

/** The aggregate payload without any field-level value. Sensitive fields become `null`. */
internal fun EntrySecret.toBundleSecret(): EntrySecret {
    val credential =
        when (val value = credential) {
            EntryCredential.None -> {
                value
            }

            is LoginCredential -> {
                value.copy(password = null)
            }

            is CardCredential -> {
                value.copy(cardNumber = null, cardCvv = null, paymentPin = null)
            }

            is IdentityCredential -> {
                value.copy(idNumber = null, seedPhrase = null, recoveryCodes = emptyList())
            }

            is SshCredential -> {
                value.copy(privateKey = null, passphrase = null)
            }

            is WifiCredential -> {
                value
            }

            is PasskeyCredential -> {
                value.copy(privateKeyReference = null)
            }

            is OtpCredential -> {
                value.copy(config = value.config.withSecret(null))
            }
        }
    return copy(credential = credential)
}

/** Reassembles a complete secret from the bundle and decrypted field-level values. */
internal fun EntrySecret.mergeSensitiveFields(fields: Map<SensitiveFieldKey, String>): EntrySecret {
    val credential =
        when (val value = credential) {
            EntryCredential.None -> {
                value
            }

            is LoginCredential -> {
                fields[SensitiveFieldKey.PASSWORD]?.let { value.copy(password = it) } ?: value
            }

            is CardCredential -> {
                value.copy(
                    cardNumber = fields[SensitiveFieldKey.CARD_NUMBER] ?: value.cardNumber,
                    cardCvv = fields[SensitiveFieldKey.CARD_CVV] ?: value.cardCvv,
                    paymentPin = fields[SensitiveFieldKey.CARD_PAYMENT_PIN] ?: value.paymentPin,
                )
            }

            is IdentityCredential -> {
                value.copy(
                    idNumber = fields[SensitiveFieldKey.IDENTITY_NUMBER] ?: value.idNumber,
                    seedPhrase = fields[SensitiveFieldKey.SEED_PHRASE] ?: value.seedPhrase,
                    recoveryCodes = fields[SensitiveFieldKey.RECOVERY_CODES]?.lines() ?: value.recoveryCodes,
                )
            }

            is SshCredential -> {
                value.copy(
                    privateKey = fields[SensitiveFieldKey.SSH_PRIVATE_KEY] ?: value.privateKey,
                    passphrase = fields[SensitiveFieldKey.SSH_PASSPHRASE] ?: value.passphrase,
                )
            }

            is WifiCredential -> {
                value
            }

            is PasskeyCredential -> {
                fields[SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE]?.let {
                    value.copy(privateKeyReference = it)
                } ?: value
            }

            is OtpCredential -> {
                fields[SensitiveFieldKey.OTP_SECRET]?.let {
                    value.copy(config = value.config.withSecret(it))
                } ?: value
            }
        }
    return copy(credential = credential)
}
