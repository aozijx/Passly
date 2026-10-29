package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SshCredentialPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.SshCredential

internal class SshEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.SSH

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is SshCredential) { "SSH adapter requires SshCredential" }
        return SecretPayload(ssh = SshCredentialPayload(
            privateKey = credential.privateKey,
            publicKey = credential.publicKey,
            passphrase = credential.passphrase,
        ))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val ssh = requireNotNull(payload.ssh) { "SSH payload is missing" }
        return SshCredential(
            privateKey = ssh.privateKey,
            publicKey = ssh.publicKey,
            passphrase = ssh.passphrase,
        )
    }
}
