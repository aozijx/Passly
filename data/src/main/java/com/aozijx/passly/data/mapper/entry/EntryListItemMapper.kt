package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryUsage

object EntryListItemMapper {

    fun assemble(
        entity: EntryEntity,
        summary: EntryProfile
    ): EntryListItem = EntryListItem(
        identity = entity.toDomainIdentity(),
        profile = summary,
        usage = EntryUsage(),
        capabilities = entity.capabilityFlags.toEntryCapabilities(),
        otpType = entity.otpType?.let(OtpType::valueOf),
    )
}
