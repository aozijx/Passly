package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.domain.model.lookup.LookupFieldValue
import com.aozijx.passly.security.search.BlindIndexRecord
import com.aozijx.passly.security.search.BlindIndexer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 盲索引协作器。
 *
 * 封装盲索引的构建与清理，供各 Command Executor 在事务内调用。
 */
@Singleton
class EntryBlindIndexHelper @Inject constructor(
    private val blindIndexer: BlindIndexer
) {

    /**
     * 删除条目的旧索引并重建。
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
        if (indexRecords.isNotEmpty()) {
            with(db) {
                searchTokenCommandDao().upsertAllForImport(indexRecords.toEntityList())
            }
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

    internal companion object {

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
}
