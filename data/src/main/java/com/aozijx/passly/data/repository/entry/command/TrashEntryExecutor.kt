package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntrySearchIndexWriter
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import javax.inject.Inject

/**
 * 移入回收站（软删除）事务执行器。
 *
 * 原子写入：Metadata 软删除 + 盲索引清理 + 活动记录。
 */
internal class TrashEntryExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val searchIndexWriter: EntrySearchIndexWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock
) {
    suspend fun execute(id: String, expectedVersion: Int): AppResult<Unit> =
        databaseTransactions.write("entry_move_to_trash") {
            val now = clock.now()
            val affected = entryCommandDao().optimisticSoftDelete(id, expectedVersion, now, now)
            databaseTransactions.checkAffectedRows(affected)

            searchIndexWriter.deleteForEntry(this, id)
            activityWriter.recordActivity(this, id, ActivityType.DELETE, now)
        }
}
