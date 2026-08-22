package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.entry.payload.EntryAssociationsPayload
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.data.local.database.entity.EntryEntity

object EntryProfileMapper {

    fun fromEntity(entity: EntryEntity): EntryProfile = EntryProfile(
        title = entity.title,
        username = entity.username,
        associations = EntryAssociations(
            primaryUrl = entity.primaryUrl,
            domains = entity.domains,
            applicationIds = entity.applicationIds,
        ),
        icon = EntryIcon(
            name = entity.iconName,
            customReference = entity.iconCustomReference,
            color = entity.iconColor,
        ),
        favorite = entity.favorite,
        tags = entity.tags,
        expiresAtMs = entity.expiresAt,
    )

    fun applyToEntity(profile: EntryProfile, entity: EntryEntity): EntryEntity = entity.copy(
        title = profile.title,
        username = profile.username,
        primaryUrl = profile.associations.primaryUrl,
        domains = profile.associations.domains,
        applicationIds = profile.associations.applicationIds,
        iconName = profile.icon.name,
        iconCustomReference = profile.icon.customReference,
        favorite = profile.favorite,
        tags = profile.tags,
        iconColor = profile.icon.color,
        expiresAt = profile.expiresAtMs,
    )

    fun toPayload(summary: EntryProfile): SummaryPayload = SummaryPayload(
        title = summary.title,
        username = summary.username,
        website = summary.associations.takeUnless { association ->
            association.primaryUrl == null && association.domains.isEmpty() &&
                association.applicationIds.isEmpty()
        }?.let { w ->
            EntryAssociationsPayload(
                primaryUrl = w.primaryUrl,
                matchDomains = w.domains,
                packageNames = w.applicationIds
            )
        },
        icon = summary.icon.name,
        iconCustomPath = summary.icon.customReference,
        favorite = summary.favorite,
        tags = summary.tags.toList(),
        color = summary.icon.color,
        expiresAt = summary.expiresAtMs
    )

    fun toDomain(payload: SummaryPayload): EntryProfile = EntryProfile(
        title = payload.title,
        username = payload.username,
        associations = payload.website?.let { w ->
            EntryAssociations(
                primaryUrl = w.primaryUrl,
                domains = w.matchDomains.toSet(),
                applicationIds = w.packageNames.toSet()
            )
        } ?: EntryAssociations(),
        icon = EntryIcon(
            name = payload.icon,
            customReference = payload.iconCustomPath,
            color = payload.color,
        ),
        favorite = payload.favorite,
        tags = payload.tags.toSet(),
        expiresAtMs = payload.expiresAt
    )
}
