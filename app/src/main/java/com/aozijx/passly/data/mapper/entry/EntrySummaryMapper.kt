package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.payload.summary.WebsiteInfoPayload
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.WebsiteInfo

object EntrySummaryMapper {

    fun toPayload(summary: EntrySummary): SummaryPayload = SummaryPayload(
        title = summary.title,
        username = summary.username,
        website = summary.website?.let { w ->
            WebsiteInfoPayload(
                primaryUrl = w.primaryUrl,
                matchDomains = w.matchDomains,
                packageNames = w.packageNames
            )
        },
        icon = summary.icon,
        iconCustomPath = summary.iconCustomPath,
        favorite = summary.favorite,
        tags = summary.tags,
        color = summary.color,
        expiresAt = summary.expiresAt
    )

    fun toDomain(payload: SummaryPayload): EntrySummary = EntrySummary(
        title = payload.title,
        username = payload.username,
        website = payload.website?.let { w ->
            WebsiteInfo(
                primaryUrl = w.primaryUrl,
                matchDomains = w.matchDomains,
                packageNames = w.packageNames
            )
        },
        icon = payload.icon,
        iconCustomPath = payload.iconCustomPath,
        favorite = payload.favorite,
        tags = payload.tags,
        color = payload.color,
        expiresAt = payload.expiresAt
    )
}
