package com.aozijx.passly.data.mapper.credential

import com.aozijx.passly.data.model.payload.credential.SshPayload
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toSshPayload(): SshPayload? {
    if (sshPrivateKey.isNullOrBlank() && cryptoSeedPhrase.isNullOrBlank()) {
        return null
    }
    return SshPayload(
        privateKey = sshPrivateKey,
        seedPhrase = cryptoSeedPhrase
    )
}

fun VaultEntry.mergeSsh(payload: SshPayload?): VaultEntry {
    payload ?: return this
    return copy(
        sshPrivateKey = payload.privateKey,
        cryptoSeedPhrase = payload.seedPhrase
    )
}