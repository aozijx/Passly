package com.aozijx.passly.data.local.database.query

import androidx.sqlite.db.SimpleSQLiteQuery
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.domain.entry.model.query.LookupField
import com.aozijx.passly.security.search.SearchToken

/**
 * 多 Token 交集查询构建器。
 *
 * 为每个搜索令牌生成一个 INTERSECT 子查询，
 * 在 SQL 层完成交集，避免在应用层加载大量候选结果。
 *
 * @param tokens 搜索令牌列表（至少一个）
 * @param fields 搜索覆盖的字段列表
 */
fun buildEntryIdIntersectionQuery(
    tokens: List<SearchToken>,
    fields: List<LookupField>
): SimpleSQLiteQuery {
    val table = DatabaseSchema.TABLE_SEARCH_TOKENS
    val fieldPlaceholders = fields.joinToString(", ") { "?" }
    val subqueries = tokens.map { token ->
        "SELECT entryId FROM $table WHERE keywordHash = ? AND gramLength = ? AND field IN ($fieldPlaceholders)"
    }
    val sql = subqueries.joinToString("\nINTERSECT\n")

    val bindArgs = mutableListOf<Any>()
    for (token in tokens) {
        bindArgs.add(token.hash)
        bindArgs.add(token.length)
        bindArgs.addAll(fields.map { it.name })
    }

    return SimpleSQLiteQuery(sql, bindArgs.toTypedArray())
}

/**
 * Autofill lookup variant: orders matched active entries by recency and caps
 * the ID set before any encrypted blobs are loaded or decrypted.
 */
fun buildRecentEntryIdIntersectionQuery(
    tokens: List<SearchToken>,
    fields: List<LookupField>,
    limit: Int,
): SimpleSQLiteQuery {
    val table = DatabaseSchema.TABLE_SEARCH_TOKENS
    val fieldPlaceholders = fields.joinToString(", ") { "?" }
    val intersection = tokens.joinToString("\nINTERSECT\n") {
        "SELECT entryId FROM $table WHERE keywordHash = ? AND gramLength = ? " +
                "AND field IN ($fieldPlaceholders)"
    }
    val sql = """
        SELECT matched.entryId
        FROM ($intersection) AS matched
        INNER JOIN ${DatabaseSchema.TABLE_ENTRIES} AS entry
            ON entry.entryId = matched.entryId
        WHERE entry.deletedAt IS NULL
        ORDER BY entry.updatedAt DESC
        LIMIT ?
    """.trimIndent()
    val args = mutableListOf<Any>()
    for (token in tokens) {
        args.add(token.hash)
        args.add(token.length)
        args.addAll(fields.map { it.name })
    }
    args.add(limit)
    return SimpleSQLiteQuery(sql, args.toTypedArray())
}
