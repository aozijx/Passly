package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.data.mapper.search.toLookupFields
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntrySearchIndexWriter
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import javax.inject.Inject

/**
 * 恢复回收站条目事务执行器。
 *
 * 使用乐观锁版本校验，原子写入：恢复 + 版本自增 + 盲索引重建 + 活动记录。
 */
internal class RestoreEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val searchIndexWriter: EntrySearchIndexWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock
) {
    suspend fun execute(id: String, expectedVersion: Int): AppResult<Unit> =
        databaseTransactions.write("entry.restore") {
            val now = clock.now()
            val affected = entryCommandDao().restoreOptimistic(id, expectedVersion, now)
            databaseTransactions.checkAffectedRows(affected)

            // 重建盲索引
            val metaEntity = entryQueryDao().getById(id)
                ?: throw NotFound()
            val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
            val vaultEntry = EntryAggregateAssembler.assembleFromDatabase(
                metaEntity, summary, null
            )
            searchIndexWriter.rebuildForEntry(this, id, vaultEntry.toLookupFields())

            activityWriter.recordActivity(this, id, ActivityType.RESTORE, now)
        }
}
