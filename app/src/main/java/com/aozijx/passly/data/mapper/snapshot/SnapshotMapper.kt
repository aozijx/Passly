package com.aozijx.passly.data.mapper.snapshot

import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.entry.VaultEntry

fun VaultEntry.toSnapshot(): VaultSnapshot = VaultSnapshot(
    id = id,
    entryType = entryType,
    revision = entryVersion,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastUsedAt = lastUsedAt,
    metadata = metadata,
    credential = credential
)

fun VaultSnapshot.toDomain(): VaultEntry = VaultEntryAssembler.assembleFromSnapshot(this)
