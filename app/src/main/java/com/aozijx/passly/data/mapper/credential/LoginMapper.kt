package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.LoginPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toLoginPayload(): LoginPayload? {
    if (username.isBlank() && password.isBlank() && email.isNullOrBlank() && notes.isNullOrBlank()) {
        return null
    }
    return LoginPayload(
        username = username,
        password = password,
        email = email,
        notes = notes
    )
}

fun VaultEntry.mergeLogin(payload: LoginPayload?): VaultEntry {
    payload ?: return this
    return copy(
        username = payload.username,
        password = payload.password,
        email = payload.email,
        notes = payload.notes
    )
}