package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.entry.command.CreateEntryExecutor
import com.aozijx.passly.data.repository.entry.command.DeleteEntryPermanentlyExecutor
import com.aozijx.passly.data.repository.entry.command.EmptyTrashExecutor
import com.aozijx.passly.data.repository.entry.command.RestoreEntryExecutor
import com.aozijx.passly.data.repository.entry.command.TrashEntryExecutor
import com.aozijx.passly.data.repository.entry.command.UpdateEntryExecutor
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 条目命令入口。
 *
 * 每个命令委托给对应的事务执行器，自身仅作为分发层：
 * - [CreateEntryExecutor] — 创建
 * - [UpdateEntryExecutor] — 更新
 * - [TrashEntryExecutor] — 移入回收站
 * - [RestoreEntryExecutor] — 恢复
 * - [DeleteEntryPermanentlyExecutor] — 永久删除
 * - [EmptyTrashExecutor] — 清空回收站
 *
 * 事务入口统一由 [DatabaseTransactionRunner] 管理，执行器不直接引用 DAO。
 */
@Singleton
internal class RoomEntryCommandRepository @Inject constructor(
    private val createEntryExecutor: CreateEntryExecutor,
    private val updateEntryExecutor: UpdateEntryExecutor,
    private val trashEntryExecutor: TrashEntryExecutor,
    private val restoreEntryExecutor: RestoreEntryExecutor,
    private val deleteEntryPermanentlyExecutor: DeleteEntryPermanentlyExecutor,
    private val emptyTrashExecutor: EmptyTrashExecutor
) : EntryCommandRepository {

    override suspend fun createEntry(entry: Entry): AppResult<EntryId> =
        createEntryExecutor.execute(entry)

    override suspend fun updateEntry(
        id: EntryId,
        expectedVersion: EntryVersion,
        changes: EntryUpdate
    ): AppResult<Unit> = updateEntryExecutor.execute(id.value, expectedVersion.value, changes)

    override suspend fun moveToTrash(
        id: EntryId,
        expectedVersion: EntryVersion
    ): AppResult<Unit> = trashEntryExecutor.execute(id.value, expectedVersion.value)

    override suspend fun restoreEntry(
        id: EntryId,
        expectedVersion: EntryVersion
    ): AppResult<Unit> = restoreEntryExecutor.execute(id.value, expectedVersion.value)

    override suspend fun deletePermanently(
        id: EntryId,
        expectedVersion: EntryVersion
    ): AppResult<Unit> = deleteEntryPermanentlyExecutor.execute(id.value, expectedVersion.value)

    override suspend fun emptyTrash(): AppResult<Int> = emptyTrashExecutor.execute()
}
