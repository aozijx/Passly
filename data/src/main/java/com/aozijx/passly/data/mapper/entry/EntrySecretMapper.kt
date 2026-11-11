package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.OtpConfigPayload
import com.aozijx.passly.data.codec.entry.payload.OtpCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.data.codec.otpHashAlgorithmFromStorageKey
import com.aozijx.passly.data.codec.otpSecretEncodingFromStorageKey
import com.aozijx.passly.data.codec.otpTypeFromStorageKey
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityTypeParseResult
import com.aozijx.passly.domain.entry.model.otp.OtpConfig

object EntrySecretMapper {
    fun toPayload(
        secret: EntrySecret,
        expectedType: EntryType,
    ): SecretPayload {
        val credential = secret.credential
        require(credential.kind == expectedType.credentialKind) {
            "Credential kind ${credential.kind} does not match entry type $expectedType"
        }
        val credentialPayload =
            when (credential) {
                EntryCredential.None -> {
                    SecretPayload()
                }

                is LoginCredential -> {
                    SecretPayload(
                        login = LoginCredentialPayload(email = credential.email, password = credential.password),
                    )
                }

                is CardCredential -> {
                    SecretPayload(
                        card =
                            CardCredentialPayload(
                                cardType = credential.cardType,
                                cardNumber = credential.cardNumber,
                                cardExpiry = credential.cardExpiry,
                                cardCvv = credential.cardCvv,
                                cardHolder = credential.cardHolder,
                                paymentPin = credential.paymentPin,
                                paymentPlatform = credential.paymentPlatform,
                                billingAddress = credential.billingAddress,
                                issuingBank = credential.issuingBank,
                                bankBranch = credential.bankBranch,
                                cardProduct = credential.cardProduct,
                                currency = credential.currency,
                                bankPhone = credential.bankPhone,
                            ),
                    )
                }

                is IdentityCredential -> {
                    SecretPayload(
                        identity =
                            IdentityCredentialPayload(
                                idNumber = credential.idNumber,
                                securityQuestion = credential.securityQuestion,
                                securityAnswer = credential.securityAnswer,
                                seedPhrase = credential.seedPhrase,
                                recoveryCodes = credential.recoveryCodes,
                            ),
                    )
                }

                is SshCredential -> {
                    SecretPayload(
                        ssh =
                            SshCredentialPayload(
                                privateKey = credential.privateKey,
                                publicKey = credential.publicKey,
                                passphrase = credential.passphrase,
                            ),
                    )
                }

                is WifiCredential -> {
                    SecretPayload(
                        wifi =
                            WifiCredentialPayload(
                                ssid = credential.ssid,
                                password = credential.password,
                                securityType = credential.securityType.storedValue,
                                isHidden = credential.isHidden,
                            ),
                    )
                }

                is PasskeyCredential -> {
                    SecretPayload(
                        passkey =
                            PasskeyCredentialPayload(
                                credentialId = credential.credentialId,
                                rpId = credential.rpId,
                                userHandle = credential.userHandle,
                                privateKeyReference = credential.privateKeyReference,
                                hardwareKeyInfo = credential.hardwareKeyInfo,
                            ),
                    )
                }

                is OtpCredential -> {
                    SecretPayload(otp = OtpCredentialPayload(credential.config.toPayload()))
                }
            }
        return credentialPayload.copy(
            notes = secret.notes,
            customFields =
                secret.customFields.map {
                    CustomFieldPayload(it.name, it.value, if (it.kind == CustomFieldKind.HIDDEN) 1 else 0)
                },
        )
    }

    fun toDomain(
        payload: SecretPayload,
        expectedType: EntryType,
    ): EntrySecret {
        require(payload.schemaVersion == 1) { "Unsupported secret payload schema version: ${payload.schemaVersion}" }
        val slotKinds = payload.credentialSlotKinds()
        require(slotKinds.size <= 1) { "Secret payload contains multiple credential kinds" }
        val actualKind = slotKinds.singleOrNull() ?: EntryCredentialKind.NONE
        require(actualKind == expectedType.credentialKind) {
            "Secret payload credential kind $actualKind does not match entry type $expectedType"
        }
        return EntrySecret(
            credential = payload.toCredential(expectedType.credentialKind),
            notes = payload.notes,
            customFields =
                payload.customFields.map {
                    CustomField(
                        name = it.name,
                        value = it.value,
                        kind = if (it.type == 1) CustomFieldKind.HIDDEN else CustomFieldKind.TEXT,
                    )
                },
        )
    }

    private fun SecretPayload.toCredential(expectedKind: EntryCredentialKind): EntryCredential =
        when (expectedKind) {
            EntryCredentialKind.NONE -> {
                EntryCredential.None
            }

            EntryCredentialKind.LOGIN -> {
                val credential = requireNotNull(login) { "Login payload is missing" }
                LoginCredential(credential.email, credential.password)
            }

            EntryCredentialKind.CARD -> {
                val credential = requireNotNull(card) { "Card payload is missing" }
                CardCredential(
                    cardType = credential.cardType,
                    cardNumber = credential.cardNumber,
                    cardExpiry = credential.cardExpiry,
                    cardCvv = credential.cardCvv,
                    cardHolder = credential.cardHolder,
                    paymentPin = credential.paymentPin,
                    paymentPlatform = credential.paymentPlatform,
                    billingAddress = credential.billingAddress,
                    issuingBank = credential.issuingBank,
                    bankBranch = credential.bankBranch,
                    cardProduct = credential.cardProduct,
                    currency = credential.currency,
                    bankPhone = credential.bankPhone,
                )
            }

            EntryCredentialKind.IDENTITY -> {
                val credential = requireNotNull(identity) { "Identity payload is missing" }
                IdentityCredential(
                    credential.idNumber,
                    credential.securityQuestion,
                    credential.securityAnswer,
                    credential.seedPhrase,
                    credential.recoveryCodes,
                )
            }

            EntryCredentialKind.SSH -> {
                val credential = requireNotNull(ssh) { "SSH payload is missing" }
                SshCredential(credential.privateKey, credential.publicKey, credential.passphrase)
            }

            EntryCredentialKind.WIFI -> {
                val credential = requireNotNull(wifi) { "Wi-Fi payload is missing" }
                WifiCredential(
                    ssid = credential.ssid,
                    password = credential.password,
                    securityType = credential.securityType.requireWifiSecurityType(),
                    isHidden = credential.isHidden,
                )
            }

            EntryCredentialKind.PASSKEY -> {
                val credential = requireNotNull(passkey) { "Passkey payload is missing" }
                PasskeyCredential(
                    credential.credentialId,
                    credential.rpId,
                    credential.userHandle,
                    credential.privateKeyReference,
                    credential.hardwareKeyInfo,
                )
            }

            EntryCredentialKind.OTP -> {
                val credential = requireNotNull(otp) { "OTP payload is missing" }
                val config = requireNotNull(credential.config) { "OTP config payload is missing" }
                OtpCredential(config.toDomain())
            }
        }

    private fun SecretPayload.credentialSlotKinds(): List<EntryCredentialKind> =
        buildList {
            if (login != null) add(EntryCredentialKind.LOGIN)
            if (card != null) add(EntryCredentialKind.CARD)
            if (identity != null) add(EntryCredentialKind.IDENTITY)
            if (ssh != null) add(EntryCredentialKind.SSH)
            if (wifi != null) add(EntryCredentialKind.WIFI)
            if (passkey != null) add(EntryCredentialKind.PASSKEY)
            if (otp != null) add(EntryCredentialKind.OTP)
        }

    private fun OtpConfig.toPayload() =
        OtpConfigPayload(
            type = type.toStorageKey(),
            secret = secret,
            algorithm = algorithm.toStorageKey(),
            digits = digits,
            periodSeconds = periodSeconds,
            counter = counter,
            encoding = encoding.toStorageKey(),
            issuer = issuer,
            accountName = accountName,
        )

    private fun OtpConfigPayload.toDomain() =
        OtpConfig(
            type = otpTypeFromStorageKey(type),
            secret = secret,
            algorithm = otpHashAlgorithmFromStorageKey(algorithm),
            digits = digits,
            periodSeconds = periodSeconds,
            counter = counter,
            encoding = otpSecretEncodingFromStorageKey(encoding),
            issuer = issuer,
            accountName = accountName,
        )

    private fun String?.requireWifiSecurityType(): WifiSecurityType =
        when (val result = WifiSecurityType.parseStoredValue(this)) {
            is WifiSecurityTypeParseResult.Supported -> {
                result.value
            }

            WifiSecurityTypeParseResult.Missing -> {
                throw IllegalArgumentException("Wi-Fi security type is missing")
            }

            is WifiSecurityTypeParseResult.Unsupported -> {
                throw IllegalArgumentException("Unsupported Wi-Fi security type: ${result.storedValue}")
            }
        }
}
