package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.CustomFieldPayload
import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
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

object EntrySecretMapper {
    fun toPayload(secret: EntrySecret, expectedType: EntryType): SecretPayload {
        val credential = secret.credential
        require(credential.kind == expectedType.credentialKind) {
            "Credential kind ${credential.kind} does not match entry type $expectedType"
        }
        val credentialPayload = when (credential) {
            EntryCredential.None -> SecretPayload()
            is LoginCredential -> entryPayloadAdapters.get(EntryCredentialKind.LOGIN).encode(credential)
            is CardCredential -> entryPayloadAdapters.get(EntryCredentialKind.CARD).encode(credential)
            is IdentityCredential -> entryPayloadAdapters.get(EntryCredentialKind.IDENTITY).encode(credential)
            is SshCredential -> SecretPayload(ssh = SshCredentialPayload(
                credential.privateKey, credential.publicKey, credential.passphrase,
            ))
            is WifiCredential -> entryPayloadAdapters.get(EntryCredentialKind.WIFI).encode(credential)
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
            EntryCredentialKind.LOGIN -> entryPayloadAdapters.get(EntryCredentialKind.LOGIN).decode(this)
            EntryCredentialKind.CARD -> entryPayloadAdapters.get(EntryCredentialKind.CARD).decode(this)
            EntryCredentialKind.IDENTITY -> entryPayloadAdapters.get(EntryCredentialKind.IDENTITY).decode(this)
            EntryCredentialKind.SSH -> requireNotNull(ssh) { "SSH payload is missing" }.let {
                SshCredential(it.privateKey, it.publicKey, it.passphrase)
            }
            EntryCredentialKind.WIFI -> entryPayloadAdapters.get(EntryCredentialKind.WIFI).decode(this)
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
}
