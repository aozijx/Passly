package com.aozijx.passly.domain.entry.model.credential


data class SshCredential(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.SSH
}
