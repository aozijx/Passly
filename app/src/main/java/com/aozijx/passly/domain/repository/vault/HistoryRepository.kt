package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.core.VaultHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>>
    suspend fun insertHistory(history: VaultHistory)
    suspend fun clearHistoryByEntryId(entryId: Int)
}
