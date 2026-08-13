package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryTimestamps

object EntryAssembler {

    fun assembleFromDatabase(
        entity: EntryEntity,
        summary: EntryProfile,
        secret: EntrySecret?,
    ): Entry {
        return Entry(
            identity = EntryIdentity(
                id = EntryId(entity.entryId),
                type = entity.entryType,
                version = EntryVersion(entity.version),
                timestamps = EntryTimestamps(
                    createdAtMs = entity.createdAt,
                    updatedAtMs = entity.updatedAt,
                    deletedAtMs = entity.deletedAt,
                ),
            ),
            profile = summary,
            secret = secret ?: EntrySecret(),
        )
    }
}
