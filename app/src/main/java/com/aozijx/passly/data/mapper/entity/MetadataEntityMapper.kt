package com.aozijx.passly.data.mapper.entity

import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toMetadataEntity(metaBlob: ByteArray): VaultMetadataEntity = VaultMetadataEntity(
    entryId = id,
    vaultId = vaultId,
    entryVersion = entryVersion,
    entryType = EntryType.fromValue(entryType),
    metadataBlob = metaBlob,
    createdAt = createdAt ?: System.currentTimeMillis(),
    updatedAt = updatedAt ?: System.currentTimeMillis(),
    deletedAt = deletedAt
)