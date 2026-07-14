package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.domain.model.entry.VaultEntry

fun VaultEntry.toMetadataEntity(metaBlob: ByteArray): VaultMetadataEntity = VaultMetadataEntity(
    entryId = id,
    vaultId = vaultId,
    entryVersion = entryVersion,
    entryType = entryType,
    metadataBlob = metaBlob,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)