package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.domain.model.entry.VaultEntry

fun VaultEntry.toCredentialEntity(credBlob: ByteArray): VaultCredentialEntity =
    VaultCredentialEntity(
        entryId = id,
        credentialBlob = credBlob
    )