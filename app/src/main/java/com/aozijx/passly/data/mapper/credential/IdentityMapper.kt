package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.IdentityPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toIdentityPayload(): IdentityPayload? {
    if (idNumber.isNullOrBlank()) {
        return null
    }
    return IdentityPayload(idNumber = idNumber)
}

fun VaultEntry.mergeIdentity(payload: IdentityPayload?): VaultEntry {
    payload ?: return this
    return copy(idNumber = payload.idNumber)
}