package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 写入命令接口。
 *
 * 定义所有对 Vault 条目的写操作契约。
 * Data 层实现此接口，Domain/Feature 层依赖此接口而非具体实现。
 */
interface EntryCommands {
    suspend fun createEntry(entry: VaultEntry): AppResult<Long>
    suspend fun updateEntry(
        id: String,
        expectedVersion: Int,
        changes: EntryChanges
    ): AppResult<Unit>

    suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun restoreEntry(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun rebuildIndex(force: Boolean = false): AppResult<Int>
    suspend fun recordUsage(
        entryId: String,
        type: ActivityType = ActivityType.VIEW
    ): AppResult<Unit>
}
