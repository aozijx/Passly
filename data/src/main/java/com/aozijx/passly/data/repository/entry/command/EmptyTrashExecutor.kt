package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.command.EntryResourceCleaner
import javax.inject.Inject

/**
 * 原子清空回收站中的数据库记录，并在提交后清理对应文件资源。
 */
internal class EmptyTrashExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val resourceCleaner: EntryResourceCleaner,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {
    suspend fun execute(): AppResult<Int> {
        val result = databaseTransactions.write("entry_empty_trash") {
            val deletedEntries = entryQueryDao().getDeleted()
            val resources = deletedEntries.map { entity ->
                val summary = EntryProfileMapper.fromEntity(entity)
                DeletedEntryResources(
                    entryId = entity.entryId,
                    customIconPath = summary.icon.customReference
                )
            }
            // 与父记录删除处于同一事务；活动、附件等仍由外键级联负责。
            entryRevisionCommandDao().deleteForDeletedEntries()
            val deletedCount = entryCommandDao().deleteAllDeleted()
            attachmentGarbageCollector.scheduleInTransaction(this)
            PurgedTrash(resources = resources, deletedCount = deletedCount)
        }

        result.onSuccessSuspend {
            resourceCleaner.clean(it.resources)
        }
        return result.map(PurgedTrash::deletedCount)
    }

    private data class PurgedTrash(
        val resources: List<DeletedEntryResources>,
        val deletedCount: Int
    )
}
