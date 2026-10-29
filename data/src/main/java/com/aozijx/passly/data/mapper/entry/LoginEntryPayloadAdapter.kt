package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.LoginCredentialPayload
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.LoginCredential

internal class LoginEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.LOGIN

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is LoginCredential) { "LOGIN adapter requires LoginCredential" }
        return SecretPayload(
            login = LoginCredentialPayload(
                email = credential.email,
                password = credential.password,
            ),
        )
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val login = requireNotNull(payload.login) { "Login payload is missing" }
        return LoginCredential(email = login.email, password = login.password)
    }
}
