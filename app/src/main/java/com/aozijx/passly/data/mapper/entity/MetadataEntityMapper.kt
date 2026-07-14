package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toMetadataEntity(metaBlob: ByteArray): VaultMetadataEntity = VaultMetadataEntity(
    entryId = id,
    vaultId = vaultId,
    entryVersion = entryVersion,
    revision = entryVersion,
    entryType = entryType,
    metadataBlob = metaBlob,
    createdAt = createdAt ?: System.currentTimeMillis(),
    updatedAt = updatedAt ?: System.currentTimeMillis(),
    deletedAt = deletedAt
)