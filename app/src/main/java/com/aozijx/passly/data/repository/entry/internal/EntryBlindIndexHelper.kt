package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.domain.entry.model.lookup.LookupFieldValue
import com.aozijx.passly.security.search.BlindIndexRecord
import com.aozijx.passly.security.search.BlindIndexer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 盲索引协作器。
 *
 * 封装盲索引的构建与清理，供各 Command Executor 在事务内调用。
 * 构建索引后自动更新 [searchIndexVersion] 以标记索引状态。
 */
@Singleton
class EntryBlindIndexHelper @Inject constructor(
    private val blindIndexer: BlindIndexer
) {
    companion object {
        /**
         * 当前搜索索引版本。
         *
         * 每次更新分词算法、索引结构或权重策略时递增此版本号，
         * 系统将在下次解锁时自动重建所有搜索索引。
         */
        const val CURRENT_SEARCH_INDEX_VERSION = 1

        fun List<BlindIndexRecord>.toEntityList(): List<SearchTokenEntity> =
            map { record ->
                SearchTokenEntity(
                    entryId = record.entryId,
                    field = record.field,
                    keywordHash = record.keywordHash,
                    gramLength = record.gramLength,
                    weight = record.weight
                )
            }
    }

    /**
     * 删除条目的旧索引并重建，同时更新 searchIndexVersion。
     */
    suspend fun rebuildForEntry(
        db: AppDatabase,
        entryId: String,
        lookupFields: List<LookupFieldValue>
    ) {
        with(db) {
            searchTokenCommandDao().deleteByEntryId(entryId)
        }
        val indexRecords = blindIndexer.index(entryId, lookupFields)
        with(db) {
            if (indexRecords.isNotEmpty()) {
                searchTokenCommandDao().upsertAllForImport(indexRecords.toEntityList())
            }
            entryCommandDao().updateSearchIndexVersion(entryId, CURRENT_SEARCH_INDEX_VERSION)
        }
    }

    /**
     * 删除条目的全部盲索引（移入回收站时调用）。
     */
    suspend fun deleteForEntry(db: AppDatabase, entryId: String) {
        with(db) {
            searchTokenCommandDao().deleteByEntryId(entryId)
        }
    }
}
