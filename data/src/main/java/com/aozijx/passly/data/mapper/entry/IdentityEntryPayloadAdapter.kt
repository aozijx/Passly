package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.IdentityCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential

internal class IdentityEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.IDENTITY

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is IdentityCredential) { "IDENTITY adapter requires IdentityCredential" }
        return SecretPayload(identity = IdentityCredentialPayload(
            idNumber = credential.idNumber,
            securityQuestion = credential.securityQuestion,
            securityAnswer = credential.securityAnswer,
            seedPhrase = credential.seedPhrase,
            recoveryCodes = credential.recoveryCodes,
        ))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val identity = requireNotNull(payload.identity) { "Identity payload is missing" }
        return IdentityCredential(
            idNumber = identity.idNumber,
            securityQuestion = identity.securityQuestion,
            securityAnswer = identity.securityAnswer,
            seedPhrase = identity.seedPhrase,
            recoveryCodes = identity.recoveryCodes,
        )
    }
}
