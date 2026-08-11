package com.aozijx.passly.data.repository.entry.executor

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.entry.internal.DeletedEntryResources
import com.aozijx.passly.data.repository.entry.internal.EntryResourceCleaner
import javax.inject.Inject

/**
 * 原子清空回收站中的数据库记录，并在提交后清理对应文件资源。
 */
class EmptyTrashExecutor @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val summaryCodec: EntrySummaryCodec,
    private val resourceCleaner: EntryResourceCleaner
) {
    suspend fun execute(): AppResult<Int> {
        val result = transactionRunner.write("entry.emptyTrash") {
            val deletedEntries = entryQueryDao().getDeleted()
            val resources = deletedEntries.map { entity ->
                val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
                DeletedEntryResources(
                    entryId = entity.entryId,
                    customIconPath = summary.iconCustomPath
                )
            }
            // 与父记录删除处于同一事务；活动、附件等仍由外键级联负责。
            entryRevisionCommandDao().deleteForDeletedEntries()
            val deletedCount = entryCommandDao().deleteAllDeleted()
            PurgedTrash(resources = resources, deletedCount = deletedCount)
        }

        result.onSuccessSuspend { resourceCleaner.clean(it.resources) }
        return result.map(PurgedTrash::deletedCount)
    }

    private data class PurgedTrash(
        val resources: List<DeletedEntryResources>,
        val deletedCount: Int
    )
}
