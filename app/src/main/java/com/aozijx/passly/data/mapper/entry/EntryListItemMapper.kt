package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.lookup.EntryListItem

object EntryListItemMapper {

    fun assemble(
        entity: EntryEntity,
        summary: EntrySummary
    ): EntryListItem = EntryListItem(
        id = entity.entryId,
        entryType = entity.entryType,
        title = summary.title,
        username = summary.username,
        icon = summary.icon,
        iconCustomPath = summary.iconCustomPath,
        website = summary.website,
        favorite = summary.favorite,
        tags = summary.tags,
        color = summary.color,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
        expiresAt = summary.expiresAt,
        lastUsedAt = null,
        usageCount = 0,
        entryVersion = entity.version,
        capabilityFlags = entity.capabilityFlags,
        otpTypeName = entity.otpType ?: ""
    )
}
