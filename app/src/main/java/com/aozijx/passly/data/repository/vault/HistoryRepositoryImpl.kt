package com.aozijx.passly.data.repository.vault

import android.content.Context
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainHistoryList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val passphraseManager: DatabasePassphraseManager
) : HistoryRepository {

    private fun historyDao() =
        AppDatabase.getDatabase(appContext, passphraseManager).vaultHistoryDao()

    override fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> =
        historyDao().getHistoryByEntryId(entryId).map { it.toDomainHistoryList() }

    override suspend fun getHistoryPaged(
        entryId: Int,
        limit: Int,
        offset: Int
    ): List<VaultHistory> =
        historyDao().getHistoryPaged(entryId, limit, offset).map { it.toDomain() }

    override suspend fun countByEntryId(entryId: Int): Int =
        historyDao().countByEntryId(entryId)

    override suspend fun insertHistory(history: VaultHistory) {
        historyDao().insertHistory(history.toEntity())
    }

    override suspend fun clearHistoryByEntryId(entryId: Int) {
        historyDao().clearHistoryByEntryId(entryId)
    }

    override suspend fun clearAll() {
        historyDao().clearAll()
    }
}