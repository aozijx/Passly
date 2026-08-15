package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryVersion

/** 从数据库行构建领域身份（供聚合与列表项共用）。 */
internal fun EntryEntity.toDomainIdentity(): EntryIdentity = EntryIdentity(
    id = EntryId(entryId),
    type = entryType,
    version = EntryVersion(version),
    timestamps = EntryTimestamps(createdAt, updatedAt, deletedAt),
)

object EntryAssembler {

    fun assembleFromDatabase(
        entity: EntryEntity,
        summary: EntryProfile,
        secret: EntrySecret?,
    ): Entry = Entry(
        identity = entity.toDomainIdentity(),
        profile = summary,
        secret = secret ?: EntrySecret(),
    )
}
