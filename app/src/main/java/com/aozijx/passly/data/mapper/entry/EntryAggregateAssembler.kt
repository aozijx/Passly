package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry

object EntryAggregateAssembler {

    fun assembleFromDatabase(
        entity: EntryEntity,
        summary: EntrySummary,
        secret: EntrySecret?,
        highSensitivitySecret: EntryHighSensitivitySecret? = null
    ): VaultEntry {
        return VaultEntry(
            header = EntryHeader(
                id = EntryId(entity.entryId),
                entryType = entity.entryType,
                version = EntryVersion.fromInt(entity.version),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt,
                vaultId = entity.vaultId,
                parentEntryId = entity.parentEntryId
            ),
            summary = summary,
            secret = secret ?: EntrySecret(),
            highSensitivitySecret = highSensitivitySecret
        )
    }
}
