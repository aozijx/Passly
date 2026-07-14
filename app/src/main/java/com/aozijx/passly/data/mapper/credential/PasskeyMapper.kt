package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.PasskeyPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toPasskeyPayload(): PasskeyPayload? {
    if (passkeyCredentialId.isNullOrBlank() &&
        passkeyRpId.isNullOrBlank() &&
        passkeyUserHandle.isNullOrBlank() &&
        passkeyPrivateKeyReference.isNullOrBlank() &&
        recoveryCodes.isNullOrBlank() &&
        hardwareKeyInfo.isNullOrBlank()
    ) {
        return null
    }
    return PasskeyPayload(
        credentialId = passkeyCredentialId,
        rpId = passkeyRpId,
        userHandle = passkeyUserHandle,
        privateKeyReference = passkeyPrivateKeyReference,
        recoveryCodes = recoveryCodes,
        hardwareKeyInfo = hardwareKeyInfo
    )
}

fun VaultEntry.mergePasskey(payload: PasskeyPayload?): VaultEntry {
    payload ?: return this
    return copy(
        passkeyCredentialId = payload.credentialId,
        passkeyRpId = payload.rpId,
        passkeyUserHandle = payload.userHandle,
        passkeyPrivateKeyReference = payload.privateKeyReference,
        recoveryCodes = payload.recoveryCodes,
        hardwareKeyInfo = payload.hardwareKeyInfo
    )
}