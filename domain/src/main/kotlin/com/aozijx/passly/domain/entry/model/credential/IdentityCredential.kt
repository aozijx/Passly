package com.aozijx.passly.domain.entry.model.credential


data class IdentityCredential(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.IDENTITY
}
