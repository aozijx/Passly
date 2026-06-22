package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import kotlinx.coroutines.flow.Flow

/**
 * 核心保险库仓库：负责条目的生命周期管理 (CRUD)
 */
interface VaultRepository {
    val allEntries: Flow<List<VaultEntry>>

    fun observeByType(type: EntryType): Flow<List<VaultEntry>>
    suspend fun getEntryById(entryId: Int): VaultEntry?
    suspend fun getByType(type: EntryType): List<VaultEntry>
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun count(): Int
    suspend fun countByType(type: EntryType): Int
    suspend fun insert(entry: VaultEntry): AppResult<Long>
    suspend fun update(entry: VaultEntry): AppResult<Unit>
    suspend fun recordUsage(entryId: Int): AppResult<Unit>
    suspend fun delete(entry: VaultEntry): AppResult<Unit>
    suspend fun deleteAll(): AppResult<Unit>
}
