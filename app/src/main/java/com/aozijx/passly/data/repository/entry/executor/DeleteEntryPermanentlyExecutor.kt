package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.NotFound
import com.aozijx.passly.core.error.ValidationError
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.DeletedEntryResources
import com.aozijx.passly.data.repository.entry.internal.EntryResourceCleaner
import javax.inject.Inject

/**
 * 永久删除单个回收站条目。
 *
 * 只允许删除 deletedAt 非空且版本匹配的条目；Room 外键负责级联删除 Secret、
 * 修订、活动、附件元数据和搜索索引，事务提交后再清理附件文件及自定义图标。
 */
class DeleteEntryPermanentlyExecutor @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val resourceCleaner: EntryResourceCleaner
) {
    suspend fun execute(id: String, expectedVersion: Int): AppResult<Unit> {
        val result = transactionRunner.write("entry.deletePermanently") {
            val entity = entryQueryDao().getById(id)
                ?: throw NotFound("entry:$id not found")
            if (entity.deletedAt == null) {
                throw ValidationError("只能永久删除回收站中的条目")
            }
            transactionRunner.checkVersion(id, entity.version, expectedVersion)

            val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
            val affected = entryCommandDao().deleteDeletedOptimistic(id, expectedVersion)
            transactionRunner.checkAffectedRows(id, expectedVersion, affected)

            DeletedEntryResources(
                entryId = id,
                customIconPath = summary.iconCustomPath
            )
        }

        result.onSuccessSuspend { resourceCleaner.clean(listOf(it)) }
        return result.map { Unit }
    }
}
