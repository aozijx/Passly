package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryUsage

internal object EntryListItemMapper {
    fun fromEntity(
        entity: EntryEntity,
        usage: EntryUsage = EntryUsage(),
        accountId: EntryId? = null,
    ): EntryListItem =
        EntryListItem(
            identity = entity.toDomainIdentity(),
            profile = EntryProfileMapper.fromEntity(entity),
            usage = usage,
            capabilities = entity.capabilityFlags.toEntryCapabilities(),
            otpType = entity.otpType,
            accountId = accountId,
        )
}
