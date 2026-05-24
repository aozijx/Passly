package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.VaultHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>>
    suspend fun getHistoryPaged(entryId: Int, limit: Int, offset: Int): List<VaultHistory>
    suspend fun countByEntryId(entryId: Int): Int
    suspend fun insertHistory(history: VaultHistory)
    suspend fun clearHistoryByEntryId(entryId: Int)
    suspend fun clearAll()
}
