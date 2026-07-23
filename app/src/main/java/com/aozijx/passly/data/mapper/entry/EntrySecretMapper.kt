package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.payload.secret.CardSecretPayload
import com.aozijx.passly.data.model.payload.secret.CustomFieldPayload
import com.aozijx.passly.data.model.payload.secret.IdentitySecretPayload
import com.aozijx.passly.data.model.payload.secret.LoginSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpConfigPayload
import com.aozijx.passly.data.model.payload.secret.OtpHashAlgorithmPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretEncodingPayload
import com.aozijx.passly.data.model.payload.secret.OtpSecretPayload
import com.aozijx.passly.data.model.payload.secret.OtpTypePayload
import com.aozijx.passly.data.model.payload.secret.PasskeySecretPayload
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.secret.SshSecretPayload
import com.aozijx.passly.data.model.payload.secret.VaultDataPayload
import com.aozijx.passly.data.model.payload.secret.WifiSecretPayload
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.secret.CardSecret
import com.aozijx.passly.domain.model.entry.secret.CustomField
import com.aozijx.passly.domain.model.entry.secret.IdentitySecret
import com.aozijx.passly.domain.model.entry.secret.LoginSecret
import com.aozijx.passly.domain.model.entry.secret.OtpSecret
import com.aozijx.passly.domain.model.entry.secret.PasskeySecret
import com.aozijx.passly.domain.model.entry.secret.SshSecret
import com.aozijx.passly.domain.model.entry.secret.WifiSecret
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType

object EntrySecretMapper {

    fun toPayload(secret: EntrySecret): SecretPayload = when (secret) {
        is EntrySecret.Login -> SecretPayload.Login(
            LoginSecretPayload(
                email = secret.data.email,
                password = secret.data.password,
                notes = secret.data.notes
            )
        )

        is EntrySecret.Note -> SecretPayload.Note(secret.notes)
        is EntrySecret.Card -> SecretPayload.Card(
            CardSecretPayload(
                cardNumber = secret.data.cardNumber,
                cardExpiry = secret.data.cardExpiry,
                cardCvv = secret.data.cardCvv,
                cardHolder = secret.data.cardHolder,
                paymentPin = secret.data.paymentPin,
                paymentPlatform = secret.data.paymentPlatform
            )
        )

        is EntrySecret.Identity -> SecretPayload.Identity(
            IdentitySecretPayload(
                idNumber = secret.data.idNumber,
                securityQuestion = secret.data.securityQuestion,
                securityAnswer = secret.data.securityAnswer,
                seedPhrase = secret.data.seedPhrase,
                recoveryCodes = secret.data.recoveryCodes
            )
        )

        is EntrySecret.SshKey -> SecretPayload.SshKey(
            SshSecretPayload(
                privateKey = secret.data.privateKey,
                publicKey = secret.data.publicKey,
                passphrase = secret.data.passphrase
            )
        )

        is EntrySecret.Wifi -> SecretPayload.Wifi(
            WifiSecretPayload(
                password = secret.data.password,
                securityType = secret.data.securityType,
                isHidden = secret.data.isHidden
            )
        )

        is EntrySecret.Passkey -> SecretPayload.Passkey(
            PasskeySecretPayload(
                credentialId = secret.data.credentialId,
                rpId = secret.data.rpId,
                userHandle = secret.data.userHandle,
                privateKeyReference = secret.data.privateKeyReference,
                hardwareKeyInfo = secret.data.hardwareKeyInfo
            )
        )

        is EntrySecret.Otp -> SecretPayload.Otp(
            OtpSecretPayload(
                config = secret.data.config?.let { otp ->
                    OtpConfigPayload(
                        type = OtpTypePayload.valueOf(otp.type.name),
                        secret = otp.secret,
                        algorithm = OtpHashAlgorithmPayload.valueOf(otp.algorithm.name),
                        digits = otp.digits,
                        periodSeconds = otp.periodSeconds,
                        counter = otp.counter,
                        encoding = OtpSecretEncodingPayload.valueOf(otp.encoding.name),
                        issuer = otp.issuer,
                        accountName = otp.accountName
                    )
                }
            )
        )

        is EntrySecret.VaultData -> SecretPayload.VaultData(
            VaultDataPayload(
                customFields = secret.customFields.map { cf ->
                    CustomFieldPayload(
                        name = cf.name,
                        value = cf.value,
                        type = cf.type
                    )
                },
                notes = secret.notes
            )
        )
    }

    fun toDomain(payload: SecretPayload): EntrySecret = when (payload) {
        is SecretPayload.Login -> EntrySecret.Login(
            LoginSecret(
                email = payload.data.email,
                password = payload.data.password,
                notes = payload.data.notes
            )
        )

        is SecretPayload.Note -> EntrySecret.Note(payload.notes)
        is SecretPayload.Card -> EntrySecret.Card(
            CardSecret(
                cardNumber = payload.data.cardNumber,
                cardExpiry = payload.data.cardExpiry,
                cardCvv = payload.data.cardCvv,
                cardHolder = payload.data.cardHolder,
                paymentPin = payload.data.paymentPin,
                paymentPlatform = payload.data.paymentPlatform
            )
        )

        is SecretPayload.Identity -> EntrySecret.Identity(
            IdentitySecret(
                idNumber = payload.data.idNumber,
                securityQuestion = payload.data.securityQuestion,
                securityAnswer = payload.data.securityAnswer,
                seedPhrase = payload.data.seedPhrase,
                recoveryCodes = payload.data.recoveryCodes
            )
        )

        is SecretPayload.SshKey -> EntrySecret.SshKey(
            SshSecret(
                privateKey = payload.data.privateKey,
                publicKey = payload.data.publicKey,
                passphrase = payload.data.passphrase
            )
        )

        is SecretPayload.Wifi -> EntrySecret.Wifi(
            WifiSecret(
                password = payload.data.password,
                securityType = payload.data.securityType,
                isHidden = payload.data.isHidden
            )
        )

        is SecretPayload.Passkey -> EntrySecret.Passkey(
            PasskeySecret(
                credentialId = payload.data.credentialId,
                rpId = payload.data.rpId,
                userHandle = payload.data.userHandle,
                privateKeyReference = payload.data.privateKeyReference,
                hardwareKeyInfo = payload.data.hardwareKeyInfo
            )
        )

        is SecretPayload.Otp -> EntrySecret.Otp(
            OtpSecret(
                config = payload.data.config?.let { p ->
                    OtpConfig(
                        type = OtpType.valueOf(p.type.name),
                        secret = p.secret,
                        algorithm = OtpHashAlgorithm.valueOf(p.algorithm.name),
                        digits = p.digits,
                        periodSeconds = p.periodSeconds,
                        counter = p.counter,
                        encoding = OtpSecretEncoding.valueOf(p.encoding.name),
                        issuer = p.issuer,
                        accountName = p.accountName
                    )
                }
            )
        )

        is SecretPayload.VaultData -> EntrySecret.VaultData(
            customFields = payload.data.customFields.map { cf ->
                CustomField(
                    name = cf.name,
                    value = cf.value,
                    type = cf.type
                )
            },
            notes = payload.data.notes
        )
    }
}
