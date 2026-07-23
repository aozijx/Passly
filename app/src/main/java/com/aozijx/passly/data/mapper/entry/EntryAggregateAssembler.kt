package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry

object EntryAggregateAssembler {

    fun assembleFromDatabase(
        entity: EntryEntity,
        summary: EntrySummary,
        secret: EntrySecret?
    ): VaultEntry {
        return VaultEntry(
            header = EntryHeader(
                id = EntryId(entity.entryId),
                entryType = entity.entryType,
                version = EntryVersion.fromInt(entity.version),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            ),
            summary = summary,
            secret = secret ?: EntrySecret.VaultData()
        )
    }
}
