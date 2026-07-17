package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.history.VaultSnapshot
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    fun getHistoryByEntryId(entryId: String): Flow<List<VaultSnapshot>>
    suspend fun insertHistory(history: VaultSnapshot)
}
