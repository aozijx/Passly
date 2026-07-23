package com.aozijx.passly.data.repository.search

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.dao.scanIndexStatus
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.model.entity.SearchTokenEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.domain.repository.search.SearchIndexMaintenance
import com.aozijx.passly.security.search.BlindIndexRecord
import com.aozijx.passly.security.search.BlindIndexer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlindIndexMaintenance @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val blindIndexer: BlindIndexer
) : SearchIndexMaintenance {

    /**
     * 重建所有条目的盲索引。
     *
     * 仅在以下情况实际执行重建：
     * - 索引不完整（[scanIndexStatus.isComplete] == false）
     * - [force] = true（如备份导入后）
     *
     * 完整性通过扫描实际数据库状态判断（已索引去重条目数 vs 活跃条目数），
     * 而非硬编码版本号或总行数。
     */
    override suspend fun rebuildIndex(force: Boolean): AppResult<Int> =
        transactionRunner.write("entry.rebuildIndex") {
            if (!force) {
                val scanResult = scanIndexStatus(
                    indexedEntryCount = searchTokenQueryDao().countDistinctEntryIds(),
                    activeEntryCount = entryQueryDao().countActive()
                )
                if (scanResult.isComplete) return@write 0
            }

            val metaEntities = entryQueryDao().getActive()
            if (metaEntities.isEmpty()) return@write 0

            val entryIds = metaEntities.map { it.entryId }
            val credEntities = entrySecretQueryDao().getByEntryIds(entryIds)
            val credMap = credEntities.associateBy { it.entryId }

            vaultMaintenanceDao().clearSearchTokens()

            var indexedCount = 0
            for (metaEntity in metaEntities) {
                val credEntity = credMap[metaEntity.entryId]
                val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
                val secret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
                val entry =
                    EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
                val indexRecords = blindIndexer.index(entry.id, entry.toLookupFields())
                if (indexRecords.isNotEmpty()) {
                    searchTokenCommandDao().upsertAllForImport(indexRecords.toEntityList())
                    indexedCount++
                }
            }

            indexedCount
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
