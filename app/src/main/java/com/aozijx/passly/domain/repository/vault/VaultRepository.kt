package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry

/**
 * 核心保险库仓库：负责条目的生命周期管理 (CRUD)
 */
interface VaultRepository {
    suspend fun getEntryById(entryId: String): VaultEntry?
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun insert(entry: VaultEntry): AppResult<Long>
    suspend fun update(entry: VaultEntry): AppResult<Unit>
    suspend fun delete(entry: VaultEntry): AppResult<Unit>

    /** 递增条目使用次数并更新 lastUsedAt */
    suspend fun incrementUsage(entryId: String): AppResult<Unit>
}
