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
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode

internal fun buildEntryCategoryQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(
    """
        SELECT MIN(TRIM(CAST(tag.value AS TEXT))) AS category
        FROM entries AS entry
        CROSS JOIN json_each(entry.tags) AS tag
        WHERE entry.deletedAt IS NULL
          AND TRIM(CAST(tag.value AS TEXT)) != ''
        GROUP BY LOWER(TRIM(CAST(tag.value AS TEXT)))
        ORDER BY category COLLATE NOCASE ASC
    """.trimIndent()
)

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

    val hierarchyApplied =
        query.normalizedSearchText.isEmpty() && query.normalizedCategory == null
    when (query.hierarchyMode) {
        EntryHierarchyDisplayMode.COLLAPSED -> {
            if (hierarchyApplied) predicates += "account.accountId IS NULL"
        }
        EntryHierarchyDisplayMode.SEPARATE -> {
            predicates += "entry.entryType != ?"
            args += EntryType.ACCOUNT.name
        }
        EntryHierarchyDisplayMode.EXPANDED,
        null,
        -> Unit
    }

    val expandedHierarchy =
        hierarchyApplied && query.hierarchyMode == EntryHierarchyDisplayMode.EXPANDED
    val itemOrderParts = query.itemOrderParts()
    val orderParts = if (expandedHierarchy) {
        buildList {
            if (query.sort.pinFavorites) {
                add(groupExpression("group_entry.favorite", "entry.favorite") + " DESC")
            }
            addAll(query.sort.field.groupOrder(query.sort.direction))
            addAll(query.sort.tieBreaker.groupOrder(query.sort.tieBreaker.stableDirection))
            add(groupExpression("group_entry.entryId", "entry.entryId") + " ASC")
            add("CASE WHEN account.accountId IS NULL THEN 0 ELSE 1 END ASC")
            addAll(itemOrderParts)
        }.distinct()
    } else itemOrderParts
    val groupJoins = if (expandedHierarchy) {
        """
            LEFT JOIN entries AS group_entry ON group_entry.entryId = account.accountId
            LEFT JOIN usage AS group_usage ON group_usage.entryId = group_entry.entryId
        """.trimIndent()
    } else ""

    val sql = """
        WITH usage AS (
            SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt
            FROM entry_activities
            WHERE activityType IN ($usagePlaceholders)
            GROUP BY entryId
        ),
        account_map AS (
            SELECT
                mapped.entryId,
                COALESCE(
                    (
                        SELECT direct.targetEntryId
                        FROM entry_links AS direct
                        JOIN entries AS direct_target
                          ON direct_target.entryId = direct.targetEntryId
                         AND direct_target.deletedAt IS NULL
                        WHERE direct.sourceEntryId = mapped.entryId
                          AND direct.relationType = '${EntryRelationType.MEMBER_OF_ACCOUNT.name}'
                        ORDER BY direct.createdAt DESC, direct.linkId DESC
                        LIMIT 1
                    ),
                    (
                        SELECT recovery.targetEntryId
                        FROM entry_links AS recovery
                        JOIN entries AS recovery_target
                          ON recovery_target.entryId = recovery.targetEntryId
                         AND recovery_target.deletedAt IS NULL
                        WHERE recovery.sourceEntryId = mapped.entryId
                          AND recovery.relationType = '${EntryRelationType.RECOVERY_FOR.name}'
                        ORDER BY recovery.createdAt DESC, recovery.linkId DESC
                        LIMIT 1
                    ),
                    (
                        SELECT member.targetEntryId
                        FROM entry_links AS otp
                        JOIN entry_links AS member ON member.sourceEntryId = otp.targetEntryId
                        JOIN entries AS account_target
                          ON account_target.entryId = member.targetEntryId
                         AND account_target.deletedAt IS NULL
                        WHERE otp.sourceEntryId = mapped.entryId
                          AND otp.relationType = '${EntryRelationType.OTP_FOR.name}'
                          AND member.relationType = '${EntryRelationType.MEMBER_OF_ACCOUNT.name}'
                        ORDER BY otp.createdAt DESC, member.createdAt DESC, member.linkId DESC
                        LIMIT 1
                    )
                ) AS accountId
            FROM entries AS mapped
        )
        SELECT
            entry.*,
            COALESCE(usage.usageCount, 0) AS usageCount,
            usage.lastUsedAt AS lastUsedAt,
            account.accountId AS accountId
        FROM entries AS entry
        LEFT JOIN usage ON usage.entryId = entry.entryId
        LEFT JOIN account_map AS account ON account.entryId = entry.entryId
        $groupJoins
        WHERE ${predicates.joinToString(" AND ")}
        ORDER BY ${orderParts.joinToString(", ")}
    """.trimIndent()
    return SimpleSQLiteQuery(sql, args.toTypedArray())
}

private fun String.escapeLike(): String = replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")

private fun EntryListQuery.itemOrderParts(): List<String> = buildList {
    if (sort.pinFavorites) add("entry.favorite DESC")
    addAll(sort.field.sqlOrder(sort.direction, "entry", "usage"))
    addAll(sort.tieBreaker.sqlOrder(sort.tieBreaker.stableDirection, "entry", "usage"))
    add("entry.entryId ASC")
}.distinct()

private fun EntrySortField.sqlOrder(
    direction: SortDirection,
    entryAlias: String,
    usageAlias: String,
): List<String> = buildList {
    if (this@sqlOrder == EntrySortField.LAST_USED_AT) {
        add("$usageAlias.lastUsedAt IS NULL ASC")
    }
    add("${sqlExpression(entryAlias, usageAlias)} ${direction.name}")
}

private fun EntrySortField.groupOrder(direction: SortDirection): List<String> {
    val expression = groupExpression(
        group = sqlExpression("group_entry", "group_usage"),
        item = sqlExpression("entry", "usage"),
    )
    return buildList {
        if (this@groupOrder == EntrySortField.LAST_USED_AT) {
            add("$expression IS NULL ASC")
        }
        add("$expression ${direction.name}")
    }
}

private fun groupExpression(group: String, item: String): String =
    "CASE WHEN account.accountId IS NOT NULL THEN $group ELSE $item END"

private fun EntrySortField.sqlExpression(entryAlias: String, usageAlias: String): String =
    when (this) {
        EntrySortField.TITLE -> "$entryAlias.title COLLATE NOCASE"
        EntrySortField.CREATED_AT -> "$entryAlias.createdAt"
        EntrySortField.UPDATED_AT -> "$entryAlias.updatedAt"
        EntrySortField.LAST_USED_AT -> "$usageAlias.lastUsedAt"
        EntrySortField.USAGE_FREQUENCY -> "COALESCE($usageAlias.usageCount, 0)"
        EntrySortField.ENTRY_TYPE -> EntryType.entries.mapIndexed { index, type ->
            "WHEN '${type.name}' THEN $index"
        }.joinToString(
            prefix = "CASE $entryAlias.entryType ",
            postfix = " ELSE ${EntryType.entries.size} END",
            separator = " ",
        )
        EntrySortField.ID -> "$entryAlias.entryId"
    }
