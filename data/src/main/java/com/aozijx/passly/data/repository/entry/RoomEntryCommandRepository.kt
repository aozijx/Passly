package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.repository.entry.executor.CreateEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.DeleteEntryPermanentlyExecutor
import com.aozijx.passly.data.repository.entry.executor.EmptyTrashExecutor
import com.aozijx.passly.data.repository.entry.executor.RestoreEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.TrashEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.UpdateEntryExecutor
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
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
 * 事务入口统一由 [VaultTransactionRunner] 管理，执行器不直接引用 DAO。
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

    override suspend fun createEntry(entry: EntryAggregate): AppResult<EntryId> =
        createEntryExecutor.execute(entry)

    override suspend fun updateEntry(
        id: String,
        expectedVersion: Int,
        changes: EntryChanges
    ): AppResult<Unit> = updateEntryExecutor.execute(id, expectedVersion, changes)

    override suspend fun moveToTrash(
        id: String,
        expectedVersion: Int
    ): AppResult<Unit> = trashEntryExecutor.execute(id, expectedVersion)

    override suspend fun restoreEntry(
        id: String,
        expectedVersion: Int
    ): AppResult<Unit> = restoreEntryExecutor.execute(id, expectedVersion)

    override suspend fun deletePermanently(
        id: String,
        expectedVersion: Int
    ): AppResult<Unit> = deleteEntryPermanentlyExecutor.execute(id, expectedVersion)

    override suspend fun emptyTrash(): AppResult<Int> = emptyTrashExecutor.execute()
}
