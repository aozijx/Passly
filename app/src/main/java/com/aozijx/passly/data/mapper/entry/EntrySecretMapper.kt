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

    fun toPayload(secret: EntrySecret): SecretPayload = SecretPayload(
        login = secret.login?.let { login ->
            LoginSecretPayload(
                email = login.email,
                password = login.password
            )
        },
        notes = secret.notes,
        card = secret.card?.let { card ->
            CardSecretPayload(
                cardNumber = card.cardNumber,
                cardExpiry = card.cardExpiry,
                cardCvv = card.cardCvv,
                cardHolder = card.cardHolder,
                paymentPin = card.paymentPin,
                paymentPlatform = card.paymentPlatform
            )
        },
        identity = secret.identity?.let { identity ->
            IdentitySecretPayload(
                idNumber = identity.idNumber,
                securityQuestion = identity.securityQuestion,
                securityAnswer = identity.securityAnswer,
                seedPhrase = identity.seedPhrase,
                recoveryCodes = identity.recoveryCodes
            )
        },
        ssh = secret.ssh?.let { ssh ->
            SshSecretPayload(
                privateKey = ssh.privateKey,
                publicKey = ssh.publicKey,
                passphrase = ssh.passphrase
            )
        },
        wifi = secret.wifi?.let { wifi ->
            WifiSecretPayload(
                password = wifi.password,
                securityType = wifi.securityType,
                isHidden = wifi.isHidden
            )
        },
        passkey = secret.passkey?.let { passkey ->
            PasskeySecretPayload(
                credentialId = passkey.credentialId,
                rpId = passkey.rpId,
                userHandle = passkey.userHandle,
                privateKeyReference = passkey.privateKeyReference,
                hardwareKeyInfo = passkey.hardwareKeyInfo
            )
        },
        otp = secret.otp?.let { otp ->
            OtpSecretPayload(
                config = otp.config?.let { config ->
                    OtpConfigPayload(
                        type = OtpTypePayload.valueOf(config.type.name),
                        secret = config.secret,
                        algorithm = OtpHashAlgorithmPayload.valueOf(config.algorithm.name),
                        digits = config.digits,
                        periodSeconds = config.periodSeconds,
                        counter = config.counter,
                        encoding = OtpSecretEncodingPayload.valueOf(config.encoding.name),
                        issuer = config.issuer,
                        accountName = config.accountName
                    )
                }
            )
        },
        customFields = secret.customFields.map { cf ->
            CustomFieldPayload(
                name = cf.name,
                value = cf.value,
                type = cf.type
            )
        }
    )

    fun toDomain(payload: SecretPayload): EntrySecret = EntrySecret(
        login = payload.login?.let { login ->
            LoginSecret(
                email = login.email,
                password = login.password
            )
        },
        notes = payload.notes,
        card = payload.card?.let { card ->
            CardSecret(
                cardNumber = card.cardNumber,
                cardExpiry = card.cardExpiry,
                cardCvv = card.cardCvv,
                cardHolder = card.cardHolder,
                paymentPin = card.paymentPin,
                paymentPlatform = card.paymentPlatform
            )
        },
        identity = payload.identity?.let { identity ->
            IdentitySecret(
                idNumber = identity.idNumber,
                securityQuestion = identity.securityQuestion,
                securityAnswer = identity.securityAnswer,
                seedPhrase = identity.seedPhrase,
                recoveryCodes = identity.recoveryCodes
            )
        },
        ssh = payload.ssh?.let { ssh ->
            SshSecret(
                privateKey = ssh.privateKey,
                publicKey = ssh.publicKey,
                passphrase = ssh.passphrase
            )
        },
        wifi = payload.wifi?.let { wifi ->
            WifiSecret(
                password = wifi.password,
                securityType = wifi.securityType,
                isHidden = wifi.isHidden
            )
        },
        passkey = payload.passkey?.let { passkey ->
            PasskeySecret(
                credentialId = passkey.credentialId,
                rpId = passkey.rpId,
                userHandle = passkey.userHandle,
                privateKeyReference = passkey.privateKeyReference,
                hardwareKeyInfo = passkey.hardwareKeyInfo
            )
        },
        otp = payload.otp?.let { otp ->
            OtpSecret(
                config = otp.config?.let { p ->
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
        },
        customFields = payload.customFields.map { cf ->
            CustomField(
                name = cf.name,
                value = cf.value,
                type = cf.type
            )
        }
    )
}
