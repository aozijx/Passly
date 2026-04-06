package com.aozijx.passly.data.repository

import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.mapper.toDomainHistoryList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryDataRepository(
    private val historyDao: VaultHistoryDao
) : HistoryRepository {
    override fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> =
        historyDao.getHistoryByEntryId(entryId).map { it.toDomainHistoryList() }

    override suspend fun insertHistory(history: VaultHistory) {
        historyDao.insertHistory(history.toEntity())
    }

    override suspend fun clearHistoryByEntryId(entryId: Int) {
        historyDao.clearHistoryByEntryId(entryId)
    }
}
