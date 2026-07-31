package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.payload.secret.HighSensitivityCardSecretPayload
import com.aozijx.passly.data.model.payload.secret.HighSensitivityIdentitySecretPayload
import com.aozijx.passly.data.model.payload.secret.HighSensitivityOtpSecretPayload
import com.aozijx.passly.data.model.payload.secret.HighSensitivityPasskeySecretPayload
import com.aozijx.passly.data.model.payload.secret.HighSensitivitySecretPayload
import com.aozijx.passly.data.model.payload.secret.HighSensitivitySshSecretPayload
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityCardSecret
import com.aozijx.passly.domain.entry.model.HighSensitivityIdentitySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityOtpSecret
import com.aozijx.passly.domain.entry.model.HighSensitivityPasskeySecret
import com.aozijx.passly.domain.entry.model.HighSensitivitySshSecret

object EntryHighSensitivitySecretMapper {

    fun toPayload(secret: EntryHighSensitivitySecret): HighSensitivitySecretPayload =
        HighSensitivitySecretPayload(
            card = secret.card?.let {
                HighSensitivityCardSecretPayload(
                    cardNumber = it.cardNumber,
                    cardCvv = it.cardCvv,
                    paymentPin = it.paymentPin
                )
            },
            identity = secret.identity?.let {
                HighSensitivityIdentitySecretPayload(
                    idNumber = it.idNumber,
                    seedPhrase = it.seedPhrase,
                    recoveryCodes = it.recoveryCodes
                )
            },
            ssh = secret.ssh?.let {
                HighSensitivitySshSecretPayload(
                    privateKey = it.privateKey,
                    passphrase = it.passphrase
                )
            },
            passkey = secret.passkey?.let {
                HighSensitivityPasskeySecretPayload(
                    privateKeyReference = it.privateKeyReference
                )
            },
            otp = secret.otp?.let {
                HighSensitivityOtpSecretPayload(secret = it.secret)
            }
        )

    fun toDomain(payload: HighSensitivitySecretPayload): EntryHighSensitivitySecret =
        EntryHighSensitivitySecret(
            card = payload.card?.let {
                HighSensitivityCardSecret(
                    cardNumber = it.cardNumber,
                    cardCvv = it.cardCvv,
                    paymentPin = it.paymentPin
                )
            },
            identity = payload.identity?.let {
                HighSensitivityIdentitySecret(
                    idNumber = it.idNumber,
                    seedPhrase = it.seedPhrase,
                    recoveryCodes = it.recoveryCodes
                )
            },
            ssh = payload.ssh?.let {
                HighSensitivitySshSecret(
                    privateKey = it.privateKey,
                    passphrase = it.passphrase
                )
            },
            passkey = payload.passkey?.let {
                HighSensitivityPasskeySecret(
                    privateKeyReference = it.privateKeyReference
                )
            },
            otp = payload.otp?.let {
                HighSensitivityOtpSecret(secret = it.secret)
            }
        )
}
