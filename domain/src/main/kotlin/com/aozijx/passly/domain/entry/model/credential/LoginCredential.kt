package com.aozijx.passly.domain.entry.model.credential


data class LoginCredential(
    val email: String? = null,
    val password: String? = null
) : EntryCredential {
    override val kind: EntryCredentialKind = EntryCredentialKind.LOGIN
}
