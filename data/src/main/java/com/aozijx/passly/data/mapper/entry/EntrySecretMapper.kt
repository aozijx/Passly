package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CardCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
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

object EntrySecretMapper {
    fun toPayload(secret: EntrySecret, expectedType: EntryType): SecretPayload {
        val credential = secret.credential
        require(credential.kind == expectedType.credentialKind) {
            "Credential kind ${credential.kind} does not match entry type $expectedType"
        }
        val credentialPayload = when (credential) {
            EntryCredential.None -> SecretPayload()
            is LoginCredential -> SecretPayload(login = LoginCredentialPayload(credential.email, credential.password))
            is CardCredential -> SecretPayload(card = CardCredentialPayload(
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
            ))
            is IdentityCredential -> SecretPayload(identity = IdentityCredentialPayload(
                idNumber = credential.idNumber,
                securityQuestion = credential.securityQuestion,
                securityAnswer = credential.securityAnswer,
                seedPhrase = credential.seedPhrase,
                recoveryCodes = credential.recoveryCodes,
            ))
            is SshCredential -> SecretPayload(ssh = SshCredentialPayload(
                credential.privateKey, credential.publicKey, credential.passphrase,
            ))
            is WifiCredential -> SecretPayload(wifi = WifiCredentialPayload(
                credential.ssid,
                credential.password,
                credential.securityType.storedValue,
                credential.isHidden,
            ))
            is PasskeyCredential -> SecretPayload(passkey = PasskeyCredentialPayload(
                credential.credentialId,
                credential.rpId,
                credential.userHandle,
                credential.privateKeyReference,
                credential.hardwareKeyInfo,
            ))
            is OtpCredential -> entryPayloadAdapters.get(EntryCredentialKind.OTP).encode(credential)
        }
        return credentialPayload.copy(
            notes = secret.notes,
            customFields = secret.customFields.map {
                CustomFieldPayload(it.name, it.value, if (it.kind == CustomFieldKind.HIDDEN) 1 else 0)
            },
        )
    }

    fun toDomain(payload: SecretPayload, expectedType: EntryType): EntrySecret {
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
            customFields = payload.customFields.map {
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
            EntryCredentialKind.NONE -> EntryCredential.None
            EntryCredentialKind.LOGIN -> requireNotNull(login) { "Login payload is missing" }.let {
                LoginCredential(it.email, it.password)
            }
            EntryCredentialKind.CARD -> requireNotNull(card) { "Card payload is missing" }.let {
                CardCredential(
                    cardType = it.cardType,
                    cardNumber = it.cardNumber,
                    cardExpiry = it.cardExpiry,
                    cardCvv = it.cardCvv,
                    cardHolder = it.cardHolder,
                    paymentPin = it.paymentPin,
                    paymentPlatform = it.paymentPlatform,
                    billingAddress = it.billingAddress,
                    issuingBank = it.issuingBank,
                    bankBranch = it.bankBranch,
                    cardProduct = it.cardProduct,
                    currency = it.currency,
                    bankPhone = it.bankPhone,
                )
            }
            EntryCredentialKind.IDENTITY -> requireNotNull(identity) { "Identity payload is missing" }.let {
                IdentityCredential(
                    it.idNumber,
                    it.securityQuestion,
                    it.securityAnswer,
                    it.seedPhrase,
                    it.recoveryCodes,
                )
            }
            EntryCredentialKind.SSH -> requireNotNull(ssh) { "SSH payload is missing" }.let {
                SshCredential(it.privateKey, it.publicKey, it.passphrase)
            }
            EntryCredentialKind.WIFI -> requireNotNull(wifi) { "Wi-Fi payload is missing" }.let {
                WifiCredential(
                    it.ssid,
                    it.password,
                    it.securityType.requireWifiSecurityType(),
                    it.isHidden,
                )
            }
            EntryCredentialKind.PASSKEY -> requireNotNull(passkey) { "Passkey payload is missing" }.let {
                PasskeyCredential(
                    it.credentialId,
                    it.rpId,
                    it.userHandle,
                    it.privateKeyReference,
                    it.hardwareKeyInfo,
                )
            }
            EntryCredentialKind.OTP -> entryPayloadAdapters.get(EntryCredentialKind.OTP).decode(this)
        }

    private fun SecretPayload.credentialSlotKinds(): List<EntryCredentialKind> =
        EntryCredentialKind.entries.filter { kind ->
            when (kind) {
                EntryCredentialKind.NONE -> false
                EntryCredentialKind.LOGIN -> login != null
                EntryCredentialKind.CARD -> card != null
                EntryCredentialKind.IDENTITY -> identity != null
                EntryCredentialKind.SSH -> ssh != null
                EntryCredentialKind.WIFI -> wifi != null
                EntryCredentialKind.PASSKEY -> passkey != null
                EntryCredentialKind.OTP -> otp != null
            }
        }

    private fun String?.requireWifiSecurityType(): WifiSecurityType =
        when (val result = WifiSecurityType.parseStoredValue(this)) {
            is WifiSecurityTypeParseResult.Supported -> result.value
            WifiSecurityTypeParseResult.Missing ->
                throw IllegalArgumentException("Wi-Fi security type is missing")
            is WifiSecurityTypeParseResult.Unsupported ->
                throw IllegalArgumentException(
                    "Unsupported Wi-Fi security type: ${result.storedValue}",
                )
        }
}
