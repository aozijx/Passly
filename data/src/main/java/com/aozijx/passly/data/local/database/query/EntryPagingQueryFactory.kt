package com.aozijx.passly.data.local.database.query

import androidx.sqlite.db.SimpleSQLiteQuery
import com.aozijx.passly.data.mapper.entry.databaseFlag
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType

internal fun buildEntryPagingQuery(query: EntryListQuery): SimpleSQLiteQuery {
    val args = mutableListOf<Any>()
    val usageTypes = ActivityType.USAGE_TYPES
    val usagePlaceholders = usageTypes.joinToString(",") { "?" }
    args.addAll(usageTypes.map(ActivityType::name))

    val predicates = mutableListOf("entry.deletedAt IS NULL")
    when (query.filter) {
        EntryFilter.ALL -> Unit
        EntryFilter.PASSWORD_ONLY -> {
            predicates += "(entry.capabilityFlags & ?) != 0"
            args += databaseFlag(EntryCapability.PASSWORD)
        }
        EntryFilter.TOTP_ONLY -> {
            predicates += "(entry.capabilityFlags & ?) != 0"
            args += databaseFlag(EntryCapability.OTP)
        }
    }

    if (query.normalizedSearchText.isNotEmpty()) {
        val pattern = "%${query.normalizedSearchText.escapeLike()}%"
        predicates += """
            (
                LOWER(entry.title) LIKE ? ESCAPE '\' OR
                LOWER(entry.username) LIKE ? ESCAPE '\' OR
                LOWER(COALESCE(entry.primaryUrl, '')) LIKE ? ESCAPE '\' OR
                EXISTS (SELECT 1 FROM json_each(entry.tags) WHERE LOWER(value) LIKE ? ESCAPE '\') OR
                EXISTS (SELECT 1 FROM json_each(entry.domains) WHERE LOWER(value) LIKE ? ESCAPE '\') OR
                EXISTS (SELECT 1 FROM json_each(entry.applicationIds) WHERE LOWER(value) LIKE ? ESCAPE '\')
            )
        """.trimIndent()
        repeat(6) { args += pattern }
    }

    query.normalizedCategory?.let { category ->
        predicates += "EXISTS (SELECT 1 FROM json_each(entry.tags) WHERE LOWER(value) = ?)"
        args += category.lowercase()
    }

    val orderParts = buildList {
        if (query.sort.pinFavorites) add("entry.favorite DESC")
        addAll(query.sort.field.sqlOrder(query.sort.direction))
        addAll(query.sort.tieBreaker.sqlOrder(query.sort.tieBreaker.stableDirection))
        add("entry.entryId ASC")
    }.distinct()

    val sql = """
        SELECT
            entry.*,
            COALESCE(usage.usageCount, 0) AS usageCount,
            usage.lastUsedAt AS lastUsedAt,
            COALESCE(
                (
                    SELECT direct.targetEntryId
                    FROM entry_links AS direct
                    WHERE direct.sourceEntryId = entry.entryId
                      AND direct.relationType = '${EntryRelationType.MEMBER_OF_ACCOUNT.name}'
                    ORDER BY direct.createdAt DESC, direct.linkId DESC
                    LIMIT 1
                ),
                (
                    SELECT recovery.targetEntryId
                    FROM entry_links AS recovery
                    WHERE recovery.sourceEntryId = entry.entryId
                      AND recovery.relationType = '${EntryRelationType.RECOVERY_FOR.name}'
                    ORDER BY recovery.createdAt DESC, recovery.linkId DESC
                    LIMIT 1
                ),
                (
                    SELECT member.targetEntryId
                    FROM entry_links AS otp
                    JOIN entry_links AS member ON member.sourceEntryId = otp.targetEntryId
                    WHERE otp.sourceEntryId = entry.entryId
                      AND otp.relationType = '${EntryRelationType.OTP_FOR.name}'
                      AND member.relationType = '${EntryRelationType.MEMBER_OF_ACCOUNT.name}'
                    ORDER BY otp.createdAt DESC, member.createdAt DESC, member.linkId DESC
                    LIMIT 1
                )
            ) AS accountId
        FROM entries AS entry
        LEFT JOIN (
            SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt
            FROM entry_activities
            WHERE activityType IN ($usagePlaceholders)
            GROUP BY entryId
        ) AS usage ON usage.entryId = entry.entryId
        WHERE ${predicates.joinToString(" AND ")}
        ORDER BY ${orderParts.joinToString(", ")}
    """.trimIndent()
    return SimpleSQLiteQuery(sql, args.toTypedArray())
}

private fun String.escapeLike(): String = replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")

private fun EntrySortField.sqlOrder(direction: SortDirection): List<String> = buildList {
    if (this@sqlOrder == EntrySortField.LAST_USED_AT) {
        add("usage.lastUsedAt IS NULL ASC")
    }
    add("${sqlExpression()} ${direction.name}")
}

private fun EntrySortField.sqlExpression(): String = when (this) {
    EntrySortField.TITLE -> "entry.title COLLATE NOCASE"
    EntrySortField.CREATED_AT -> "entry.createdAt"
    EntrySortField.UPDATED_AT -> "entry.updatedAt"
    EntrySortField.LAST_USED_AT -> "usage.lastUsedAt"
    EntrySortField.USAGE_FREQUENCY -> "COALESCE(usage.usageCount, 0)"
    EntrySortField.ENTRY_TYPE -> EntryType.entries.mapIndexed { index, type ->
        "WHEN '${type.name}' THEN $index"
    }.joinToString(
        prefix = "CASE entry.entryType ",
        postfix = " ELSE ${EntryType.entries.size} END",
        separator = " ",
    )
    EntrySortField.ID -> "entry.entryId"
}
