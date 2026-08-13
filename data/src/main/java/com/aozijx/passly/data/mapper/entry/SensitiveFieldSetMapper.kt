package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityCardSecret
import com.aozijx.passly.domain.entry.model.HighSensitivityIdentitySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityOtpSecret
import com.aozijx.passly.domain.entry.model.HighSensitivityPasskeySecret
import com.aozijx.passly.domain.entry.model.HighSensitivitySshSecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun EntryHighSensitivitySecret.toSensitiveFieldValues(): Map<SensitiveFieldKey, String> =
    buildMap {
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
        putText(SensitiveFieldKey.OTP_SECRET, otp?.secret)
    }

internal fun Map<SensitiveFieldKey, String>.toHighSensitivitySecret() =
    EntryHighSensitivitySecret(
        card = HighSensitivityCardSecret(
            cardNumber = get(SensitiveFieldKey.CARD_NUMBER),
            cardCvv = get(SensitiveFieldKey.CARD_CVV),
            paymentPin = get(SensitiveFieldKey.CARD_PAYMENT_PIN)
        ).takeUnless { it == HighSensitivityCardSecret() },
        identity = HighSensitivityIdentitySecret(
            idNumber = get(SensitiveFieldKey.IDENTITY_NUMBER),
            seedPhrase = get(SensitiveFieldKey.SEED_PHRASE),
            recoveryCodes = get(SensitiveFieldKey.RECOVERY_CODES)?.lines().orEmpty()
        ).takeUnless { it == HighSensitivityIdentitySecret() },
        ssh = HighSensitivitySshSecret(
            privateKey = get(SensitiveFieldKey.SSH_PRIVATE_KEY),
            passphrase = get(SensitiveFieldKey.SSH_PASSPHRASE)
        ).takeUnless { it == HighSensitivitySshSecret() },
        passkey = HighSensitivityPasskeySecret(
            privateKeyReference = get(SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE)
        ).takeUnless { it == HighSensitivityPasskeySecret() },
        otp = HighSensitivityOtpSecret(
            secret = get(SensitiveFieldKey.OTP_SECRET)
        ).takeUnless { it == HighSensitivityOtpSecret() }
    )
