package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.PasskeyCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential

internal class PasskeyEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.PASSKEY

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is PasskeyCredential) { "PASSKEY adapter requires PasskeyCredential" }
        return SecretPayload(passkey = PasskeyCredentialPayload(
            credentialId = credential.credentialId,
            rpId = credential.rpId,
            userHandle = credential.userHandle,
            privateKeyReference = credential.privateKeyReference,
            hardwareKeyInfo = credential.hardwareKeyInfo,
        ))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val passkey = requireNotNull(payload.passkey) { "Passkey payload is missing" }
        return PasskeyCredential(
            credentialId = passkey.credentialId,
            rpId = passkey.rpId,
            userHandle = passkey.userHandle,
            privateKeyReference = passkey.privateKeyReference,
            hardwareKeyInfo = passkey.hardwareKeyInfo,
        )
    }
}
