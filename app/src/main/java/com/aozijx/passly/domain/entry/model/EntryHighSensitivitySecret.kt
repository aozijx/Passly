package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
import com.aozijx.passly.domain.entry.model.secret.PasskeySecret
import com.aozijx.passly.domain.entry.model.secret.SshSecret

data class EntryHighSensitivitySecret(
    val card: HighSensitivityCardSecret? = null,
    val identity: HighSensitivityIdentitySecret? = null,
    val ssh: HighSensitivitySshSecret? = null,
    val passkey: HighSensitivityPasskeySecret? = null,
    val otp: HighSensitivityOtpSecret? = null
) {
    val isEmpty: Boolean
        get() = this == EMPTY

    companion object {
        val EMPTY = EntryHighSensitivitySecret()
    }
}

data class HighSensitivityCardSecret(
    val cardNumber: String? = null,
    val cardCvv: String? = null,
    val paymentPin: String? = null
)

data class HighSensitivityIdentitySecret(
    val idNumber: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
)

data class HighSensitivitySshSecret(
    val privateKey: String? = null,
    val passphrase: String? = null
)

data class HighSensitivityPasskeySecret(
    val privateKeyReference: String? = null
)

data class HighSensitivityOtpSecret(
    val secret: String? = null
)

fun EntrySecret.extractHighSensitivity(): EntryHighSensitivitySecret =
    EntryHighSensitivitySecret(
        card = card?.let {
            HighSensitivityCardSecret(
                cardNumber = it.cardNumber,
                cardCvv = it.cardCvv,
                paymentPin = it.paymentPin
            )
        }?.takeUnless { it == HighSensitivityCardSecret() },
        identity = null,
        ssh = null,
        passkey = null,
        otp = null
    )

fun EntrySecret.withoutHighSensitivity(): EntrySecret =
    copy(
        card = card?.copy(
            cardNumber = null,
            cardCvv = null,
            paymentPin = null
        )
    )

fun EntrySecret.withHighSensitivity(high: EntryHighSensitivitySecret?): EntrySecret {
    if (high == null || high.isEmpty) return this
    return copy(
        card = mergeCardSecret(card, high.card),
        identity = mergeIdentitySecret(identity, high.identity),
        ssh = mergeSshSecret(ssh, high.ssh),
        passkey = mergePasskeySecret(passkey, high.passkey),
        otp = mergeOtpSecret(otp, high.otp)
    )
}

fun EntryHighSensitivitySecret.mergeWith(update: EntryHighSensitivitySecret): EntryHighSensitivitySecret =
    EntryHighSensitivitySecret(
        card = update.card ?: card,
        identity = update.identity ?: identity,
        ssh = update.ssh ?: ssh,
        passkey = update.passkey ?: passkey,
        otp = update.otp ?: otp
    )

private fun mergeCardSecret(
    secret: CardSecret?,
    high: HighSensitivityCardSecret?
): CardSecret? {
    if (secret == null && high == null) return null
    return (secret ?: CardSecret()).copy(
        cardNumber = high?.cardNumber ?: secret?.cardNumber,
        cardCvv = high?.cardCvv ?: secret?.cardCvv,
        paymentPin = high?.paymentPin ?: secret?.paymentPin
    )
}

private fun mergeIdentitySecret(
    secret: IdentitySecret?,
    high: HighSensitivityIdentitySecret?
): IdentitySecret? {
    if (secret == null && high == null) return null
    return (secret ?: IdentitySecret()).copy(
        idNumber = high?.idNumber ?: secret?.idNumber,
        seedPhrase = high?.seedPhrase ?: secret?.seedPhrase,
        recoveryCodes = high?.recoveryCodes?.takeIf { it.isNotEmpty() } ?: secret?.recoveryCodes.orEmpty()
    )
}

private fun mergeSshSecret(
    secret: SshSecret?,
    high: HighSensitivitySshSecret?
): SshSecret? {
    if (secret == null && high == null) return null
    return (secret ?: SshSecret()).copy(
        privateKey = high?.privateKey ?: secret?.privateKey,
        passphrase = high?.passphrase ?: secret?.passphrase
    )
}

private fun mergePasskeySecret(
    secret: PasskeySecret?,
    high: HighSensitivityPasskeySecret?
): PasskeySecret? {
    if (secret == null && high == null) return null
    return (secret ?: PasskeySecret()).copy(
        privateKeyReference = high?.privateKeyReference ?: secret?.privateKeyReference
    )
}

private fun mergeOtpSecret(
    secret: OtpSecret?,
    high: HighSensitivityOtpSecret?
): OtpSecret? {
    if (secret == null && high == null) return null
    val config = secret?.config ?: high?.secret?.let { OtpConfig(secret = it) }
    return (secret ?: OtpSecret()).copy(
        config = config?.copy(secret = high?.secret ?: config.secret)
    )
}
