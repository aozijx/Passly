package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainHistoryList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.repository.vault.internal.ifLockedReturn
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import com.aozijx.passly.security.crypto.LockState
import com.aozijx.passly.security.crypto.VaultLockManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSessionManager,
    private val lockManager: VaultLockManager
) : HistoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> = lockManager.lockState
        .flatMapLatest { state ->
            if (state == LockState.LOCKED) emptyFlow()
            else sessionManager.withDatabase {
                vaultHistoryDao().getHistoryByEntryId(entryId).map { it.toDomainHistoryList() }
            }
        }

    override suspend fun getHistoryPaged(
        entryId: Int,
        limit: Int,
        offset: Int
    ): List<VaultHistory> {
        lockManager.ifLockedReturn { return emptyList() }
        return sessionManager.withDatabase {
            vaultHistoryDao().getHistoryPaged(entryId, limit, offset).map { it.toDomain() }
        }
    }

    override suspend fun countByEntryId(entryId: Int): Int {
        lockManager.ifLockedReturn { return 0 }
        return sessionManager.withDatabase {
            vaultHistoryDao().countByEntryId(entryId)
        }
    }

    override suspend fun insertHistory(history: VaultHistory) {
        lockManager.ifLockedReturn { return }
        runCatching {
            sessionManager.withDatabase {
                vaultHistoryDao().insertHistory(history.toEntity())
            }
        }.onFailure {
            Logcat.e("HistoryRepo", "Failed to insert history for entry ${history.entryId}", it)
        }
    }

    override suspend fun clearHistoryByEntryId(entryId: Int) {
        lockManager.ifLockedReturn { return }
        runCatching {
            sessionManager.withDatabase {
                vaultHistoryDao().clearHistoryByEntryId(entryId)
            }
        }.onFailure {
            Logcat.e("HistoryRepo", "Failed to clear history for entry $entryId", it)
        }
    }

    override suspend fun clearAll() {
        lockManager.ifLockedReturn { return }
        runCatching {
            sessionManager.withDatabase {
                vaultHistoryDao().clearAll()
            }
        }.onFailure {
            Logcat.e("HistoryRepo", "Failed to clear all history", it)
        }
    }
}