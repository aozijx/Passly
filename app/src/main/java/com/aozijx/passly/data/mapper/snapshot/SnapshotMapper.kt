package com.aozijx.passly.data.mapper.snapshot

import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.model.payload.snapshot.VaultSnapshot
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry

fun VaultEntry.toSnapshot(): VaultSnapshot = VaultSnapshot(
    id = id,
    entryType = entryType,
    revision = entryVersion,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    summary = EntrySummaryMapper.toPayload(summary),
    secret = EntrySecretMapper.toPayload(secret)
)

fun VaultSnapshot.toDomain(): VaultEntry = VaultEntry(
    header = EntryHeader(
        id = EntryId(id),
        entryType = entryType,
        version = EntryVersion.fromInt(revision),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    ),
    summary = EntrySummaryMapper.toDomain(summary),
    secret = EntrySecretMapper.toDomain(secret)
)
