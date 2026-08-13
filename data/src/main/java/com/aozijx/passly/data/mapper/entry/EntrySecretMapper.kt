package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.OtpConfigPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialEncodingPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.OtpHashAlgorithmPayload
import com.aozijx.passly.data.codec.entry.payload.OtpTypePayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

object EntrySecretMapper {
    fun toPayload(secret: EntrySecret): SecretPayload {
        val credential = secret.credential
        return SecretPayload(
            login = (credential as? LoginCredential)?.let {
                LoginCredentialPayload(email = it.email, password = it.password)
            },
            card = (credential as? CardCredential)?.let {
                CardCredentialPayload(
                    cardType = it.cardType,
                    cardNumber = it.cardNumber,
                    cardExpiry = it.cardExpiry,
                    cardCvv = it.cardCvv,
                    cardHolder = it.cardHolder,
                    paymentPin = it.paymentPin,
                    paymentPlatform = it.paymentPlatform,
                    billingAddress = it.billingAddress,
                    hasCardNumber = !it.cardNumber.isNullOrBlank(),
                    hasCardCvv = !it.cardCvv.isNullOrBlank(),
                    hasPaymentPin = !it.paymentPin.isNullOrBlank(),
                )
            },
            identity = (credential as? IdentityCredential)?.let {
                IdentityCredentialPayload(
                    idNumber = it.idNumber,
                    securityQuestion = it.securityQuestion,
                    securityAnswer = it.securityAnswer,
                    seedPhrase = it.seedPhrase,
                    recoveryCodes = it.recoveryCodes,
                )
            },
            ssh = (credential as? SshCredential)?.let {
                SshCredentialPayload(it.privateKey, it.publicKey, it.passphrase)
            },
            wifi = (credential as? WifiCredential)?.let {
                WifiCredentialPayload(it.ssid, it.password, it.securityType, it.isHidden)
            },
            passkey = (credential as? PasskeyCredential)?.let {
                PasskeyCredentialPayload(
                    it.credentialId,
                    it.rpId,
                    it.userHandle,
                    it.privateKeyReference,
                    it.hardwareKeyInfo,
                )
            },
            otp = (credential as? OtpCredential)?.let {
                OtpCredentialPayload(it.config.toPayload())
            },
            notes = secret.notes,
            customFields = secret.customFields.map {
                CustomFieldPayload(it.name, it.value, if (it.kind == CustomFieldKind.HIDDEN) 1 else 0)
            },
        )
    }

    fun toDomain(payload: SecretPayload): EntrySecret = EntrySecret(
        credential = payload.toCredential(),
        notes = payload.notes,
        customFields = payload.customFields.map {
            CustomField(
                name = it.name,
                value = it.value,
                kind = if (it.type == 1) CustomFieldKind.HIDDEN else CustomFieldKind.TEXT,
            )
        },
    )

    private fun SecretPayload.toCredential(): EntryCredential {
        val credentials = listOfNotNull(
            login?.let { LoginCredential(it.email, it.password) },
            card?.let {
                CardCredential(
                    cardType = it.cardType,
                    cardNumber = it.cardNumber,
                    cardExpiry = it.cardExpiry,
                    cardCvv = it.cardCvv,
                    cardHolder = it.cardHolder,
                    paymentPin = it.paymentPin,
                    paymentPlatform = it.paymentPlatform,
                    billingAddress = it.billingAddress,
                )
            },
            identity?.let {
                IdentityCredential(
                    it.idNumber,
                    it.securityQuestion,
                    it.securityAnswer,
                    it.seedPhrase,
                    it.recoveryCodes,
                )
            },
            ssh?.let { SshCredential(it.privateKey, it.publicKey, it.passphrase) },
            wifi?.let { WifiCredential(it.ssid, it.password, it.securityType, it.isHidden) },
            passkey?.let {
                PasskeyCredential(
                    it.credentialId,
                    it.rpId,
                    it.userHandle,
                    it.privateKeyReference,
                    it.hardwareKeyInfo,
                )
            },
            otp?.config?.let { OtpCredential(it.toDomain()) },
        )
        require(credentials.size <= 1) { "Secret payload contains multiple credential kinds" }
        return credentials.singleOrNull() ?: EntryCredential.None
    }

    private fun OtpConfig.toPayload() = OtpConfigPayload(
        type = OtpTypePayload.valueOf(type.name),
        secret = secret,
        algorithm = OtpHashAlgorithmPayload.valueOf(algorithm.name),
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        encoding = OtpCredentialEncodingPayload.valueOf(encoding.name),
        issuer = issuer,
        accountName = accountName,
    )

    private fun OtpConfigPayload.toDomain() = OtpConfig(
        type = OtpType.valueOf(type.name),
        secret = secret,
        algorithm = OtpHashAlgorithm.valueOf(algorithm.name),
        digits = digits,
        periodSeconds = periodSeconds,
        counter = counter,
        encoding = OtpSecretEncoding.valueOf(encoding.name),
        issuer = issuer,
        accountName = accountName,
    )
}
