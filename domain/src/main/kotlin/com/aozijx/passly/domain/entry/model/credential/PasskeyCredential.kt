package com.aozijx.passly.domain.entry.model.credential


data class PasskeyCredential(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.PASSKEY
}
