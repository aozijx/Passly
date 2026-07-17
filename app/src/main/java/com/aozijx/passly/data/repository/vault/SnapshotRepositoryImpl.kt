package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.onFailureLog
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.data.model.entity.VaultSnapshotEntity
import com.aozijx.passly.domain.model.history.SnapshotType
import com.aozijx.passly.domain.model.history.VaultSnapshot
import com.aozijx.passly.domain.repository.vault.SnapshotRepository
import com.aozijx.passly.security.session.SessionStateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSession,
    private val sessionState: SessionStateProvider
) : SnapshotRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getHistoryByEntryId(entryId: String): Flow<List<VaultSnapshot>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) emptyFlow()
                else sessionManager.withDatabase {
                    historyDao().observeByEntryId(entryId)
                        .map { it.toDomainHistoryList() }
                        .flowOn(Dispatchers.IO)
                }
            }

    override suspend fun insertHistory(history: VaultSnapshot) {
        if (sessionState.isLocked()) return
        AppResult.runSuspendCatching("history.insert") {
            sessionManager.withDatabase {
                historyDao().insert(history.toEntity())
            }
        }.onFailureLog("HistoryRepo")
    }

    private fun List<VaultSnapshotEntity>.toDomainHistoryList(): List<VaultSnapshot> {
        return this.map { it.toDomain() }
    }

    private fun VaultSnapshotEntity.toDomain(): VaultSnapshot {
        return VaultSnapshot(
            snapshotId = historyId,
            entryId = entryId,
            version = version,
            createdAt = createdAt,
            changeType = SnapshotType.fromValue(changeType)
        )
    }

    private fun VaultSnapshot.toEntity(): VaultSnapshotEntity {
        return VaultSnapshotEntity(
            historyId = snapshotId,
            entryId = entryId,
            version = version,
            snapshotBlob = ByteArray(0),
            changeType = changeType.value,
            createdAt = createdAt
        )
    }
}
