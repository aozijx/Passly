package com.aozijx.passly.data.mapper.snapshot

import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.mapper.credential.toCredentialPayload
import com.aozijx.passly.data.mapper.metadata.toMetadataPayload
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.VaultEntry

fun VaultEntry.toSnapshot(): VaultSnapshot = VaultSnapshot(
    id = id,
    vaultId = vaultId,
    entryType = entryType,
    deletedAt = deletedAt,
    createdAt = createdAt ?: 0L,
    updatedAt = updatedAt ?: 0L,
    lastUsedAt = lastUsedAt,
    revision = entryVersion,
    metadata = toMetadataPayload(),
    credential = toCredentialPayload()
)

fun VaultSnapshot.toDomain(): VaultEntry = VaultEntryAssembler.assembleFromSnapshot(this)
