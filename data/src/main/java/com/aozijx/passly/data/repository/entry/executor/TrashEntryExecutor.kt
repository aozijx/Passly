package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.EntryActivityHelper
import com.aozijx.passly.data.repository.entry.internal.EntryBlindIndexHelper
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import javax.inject.Inject

/**
 * 移入回收站（软删除）事务执行器。
 *
 * 原子写入：Metadata 软删除 + 盲索引清理 + 活动记录。
 */
class TrashEntryExecutor @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val blindIndexHelper: EntryBlindIndexHelper,
    private val activityHelper: EntryActivityHelper,
    private val clock: Clock
) {
    suspend fun execute(id: String, expectedVersion: Int): AppResult<Unit> =
        transactionRunner.write("entry.moveToTrash") {
            val now = clock.now()
            val affected = entryCommandDao().optimisticSoftDelete(id, expectedVersion, now, now)
            transactionRunner.checkAffectedRows(affected)

            blindIndexHelper.deleteForEntry(this, id)
            activityHelper.recordActivity(this, id, ActivityType.DELETE, now)
        }
}
