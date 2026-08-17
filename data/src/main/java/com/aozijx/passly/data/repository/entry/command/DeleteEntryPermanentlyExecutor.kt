package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.command.EntryResourceCleaner
import javax.inject.Inject

/**
 * 永久删除单个回收站条目。
 *
 * 只允许删除 deletedAt 非空且版本匹配的条目；Room 外键负责级联删除 Secret、
 * 修订、活动、附件元数据和搜索索引，事务提交后再清理附件文件及自定义图标。
 */
internal class DeleteEntryPermanentlyExecutor @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val summaryCodec: EntryProfileCodec,
    private val resourceCleaner: EntryResourceCleaner,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {
    suspend fun execute(id: String, expectedVersion: Int): AppResult<Unit> {
        val result = databaseTransactions.write("entry_delete_permanently") {
            val entity = entryQueryDao().getById(id)
                ?: throw NotFound()
            if (entity.deletedAt == null) throw ValidationError()
            databaseTransactions.checkVersion(entity.version, expectedVersion)

            val summary = summaryCodec.decrypt(entity.summaryBlob, entity.entryId)
            // 显式清理历史，使生命周期语义不只依赖 Room 外键的隐式级联。
            // 若随后的乐观删除失败，DatabaseTransactionRunner 会回滚本次历史删除。
            entryRevisionCommandDao().deleteByEntryId(id)
            val affected = entryCommandDao().deleteDeletedOptimistic(id, expectedVersion)
            databaseTransactions.checkAffectedRows(affected)

            attachmentGarbageCollector.scheduleInTransaction(this)
            DeletedEntryResources(
                entryId = id,
                customIconPath = summary.icon.customReference,
            )
        }

        result.onSuccessSuspend {
            resourceCleaner.clean(listOf(it))
        }
        return result.map { }
    }
}
