package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.repository.entry.executor.CreateEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.RestoreEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.TrashEntryExecutor
import com.aozijx.passly.data.repository.entry.executor.UpdateEntryExecutor
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.EntryCommandRepository
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
 *
 * 事务入口统一由 [VaultTransactionRunner] 管理，执行器不直接引用 DAO。
 */
@Singleton
class RoomEntryCommandRepository @Inject constructor(
    private val createEntryExecutor: CreateEntryExecutor,
    private val updateEntryExecutor: UpdateEntryExecutor,
    private val trashEntryExecutor: TrashEntryExecutor,
    private val restoreEntryExecutor: RestoreEntryExecutor
) : EntryCommandRepository {

    override suspend fun createEntry(entry: VaultEntry): AppResult<EntryId> =
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
}
