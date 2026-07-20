package com.aozijx.passly.domain.repository.snapshot

import com.aozijx.passly.domain.model.history.VaultSnapshot
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    fun observeByEntryId(entryId: String): Flow<List<VaultSnapshot>>
    fun observeAll(): Flow<List<VaultSnapshot>>
    suspend fun getByEntryId(entryId: String): List<VaultSnapshot>
    suspend fun insert(snapshot: VaultSnapshot)
    suspend fun deleteByEntryId(entryId: String)
}
