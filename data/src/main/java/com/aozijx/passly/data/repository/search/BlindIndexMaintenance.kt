package com.aozijx.passly.data.repository.search

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.local.database.entity.SearchTokenEntity
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.entry.command.EntrySearchIndexWriter
import com.aozijx.passly.domain.entry.repository.SearchIndexMaintenance
import com.aozijx.passly.security.search.BlindIndexRecord
import com.aozijx.passly.security.search.BlindIndexer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 盲索引维护。
 *
 * 使用 [searchIndexVersion] 字段追踪每条目的索引版本，
 * 仅重建版本滞后的条目，支持分批处理。
 * 删除一条索引或升级分词算法后，版本不匹配即可自动检测并修复。
 */
@Singleton
internal class BlindIndexMaintenance @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val blindIndexer: BlindIndexer,
    private val searchIndexWriter: EntrySearchIndexWriter
) : SearchIndexMaintenance {

    companion object {
        /** 每批处理的最大条目数 */
        private const val BATCH_SIZE = 20
    }

    override suspend fun rebuildIndex(force: Boolean): AppResult<Int> =
        databaseTransactions.write("entry.rebuildIndex") {
            val staleEntryIds = if (force) {
                entryQueryDao().getActive().map { it.entryId }
            } else {
                entryQueryDao().getActiveEntryIdsNeedingIndexRebuild(
                    EntrySearchIndexWriter.CURRENT_SEARCH_INDEX_VERSION
                )
            }

            if (staleEntryIds.isEmpty()) return@write 0

            val metaEntities = entryQueryDao().getByIdsForMaintenance(staleEntryIds)
            val credEntities = entrySecretQueryDao().getByEntryIds(staleEntryIds)
            val credMap = credEntities.associateBy { it.entryId }

            var rebuiltCount = 0
            metaEntities.chunked(BATCH_SIZE).forEach { batch ->
                for (metaEntity in batch) {
                    val credEntity = credMap[metaEntity.entryId]
                    val summary = summaryCodec.decrypt(
                        metaEntity.summaryBlob, metaEntity.entryId
                    )
                    val secret = credEntity?.let {
                        secretCodec.decrypt(it.secretBlob, it.entryId)
                    }
                    val entry = EntryAggregateAssembler.assembleFromDatabase(
                        metaEntity, summary, secret
                    )
                    // rebuildForEntry 内部：删除旧索引 -> 生成新索引 -> 更新 searchIndexVersion
                    searchIndexWriter.rebuildForEntry(
                        this,
                        metaEntity.entryId,
                        entry.toLookupFields()
                    )
                    rebuiltCount++
                }
            }

            rebuiltCount
        }

    private fun List<BlindIndexRecord>.toEntityList(): List<SearchTokenEntity> =
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
