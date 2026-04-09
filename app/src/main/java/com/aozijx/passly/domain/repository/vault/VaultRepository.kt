package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.core.VaultEntry
import kotlinx.coroutines.flow.Flow

/**
 * 核心保险库仓库：负责条目的生命周期管理 (CRUD)
 */
interface VaultRepository {
    val allEntries: Flow<List<VaultEntry>>
    
    suspend fun getEntryById(entryId: Int): VaultEntry?
    suspend fun insert(entry: VaultEntry): Long
    suspend fun update(entry: VaultEntry)
    suspend fun delete(entry: VaultEntry)
    suspend fun deleteAll()
}
