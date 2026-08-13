package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.entry.payload.EntryAssociationsPayload
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon

object EntryProfileMapper {

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
