package com.aozijx.passly.data.local.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.domain.model.lookup.LookupField
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
 * 盲索引完整性扫描结果。
 *
 * 通过扫描实际数据库状态（已索引去重条目数 vs 活跃条目数）
 * 判断索引是否完整，替代硬编码版本号。
 *
 * @property isComplete 所有活跃条目均已建索引
 * @property indexedEntryCount 已索引的去重条目数
 * @property activeEntryCount 当前活跃条目总数
 */
data class IndexScanResult(
    val isComplete: Boolean,
    val indexedEntryCount: Int,
    val activeEntryCount: Int
)

/**
 * 扫描盲索引状态，判断索引是否完整。
 *
 * @param indexedEntryCount [SearchTokenDao.countDistinctEntryIds] 结果
 * @param activeEntryCount [EntryDao.countActive] 结果
 */
fun scanIndexStatus(
    indexedEntryCount: Int,
    activeEntryCount: Int
): IndexScanResult = IndexScanResult(
    isComplete = indexedEntryCount >= activeEntryCount,
    indexedEntryCount = indexedEntryCount,
    activeEntryCount = activeEntryCount
)
