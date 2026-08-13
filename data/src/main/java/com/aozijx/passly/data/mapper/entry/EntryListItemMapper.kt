package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.model.query.EntryListItem

object EntryListItemMapper {

    fun assemble(
        entity: EntryEntity,
        summary: EntryProfile
    ): EntryListItem = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(entity.entryId),
            type = entity.entryType,
            version = EntryVersion(entity.version),
            timestamps = EntryTimestamps(entity.createdAt, entity.updatedAt, entity.deletedAt),
        ),
        profile = summary,
        usage = EntryUsage(),
        capabilities = entity.capabilityFlags.toEntryCapabilities(),
        otpType = entity.otpType?.let(OtpType::valueOf),
    )
}
