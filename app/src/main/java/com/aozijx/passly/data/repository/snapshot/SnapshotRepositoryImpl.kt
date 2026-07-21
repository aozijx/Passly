package com.aozijx.passly.data.repository.snapshot

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.onFailureLog
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.model.entity.VaultSnapshotEntity
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.history.SnapshotType
import com.aozijx.passly.domain.model.history.VaultSnapshot
import com.aozijx.passly.domain.repository.snapshot.SnapshotRepository
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
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider
) : SnapshotRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<VaultSnapshot>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) emptyFlow()
                else sessionManager.observeFlow {
                    historyDao().observeByEntryId(entryId)
                        .map { it.toDomainList() }
                        .flowOn(Dispatchers.IO)
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<VaultSnapshot>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) emptyFlow()
                else sessionManager.observeFlow {
                    emptyFlow()
                }
            }

    override suspend fun getByEntryId(entryId: String): List<VaultSnapshot> {
        if (sessionState.isLocked()) return emptyList()
        return sessionManager.query {
            // No direct DAO method for getByEntryId; observe then first() is preferred
            emptyList()
        }
    }

    override suspend fun insert(snapshot: VaultSnapshot) {
        if (sessionState.isLocked()) return
        AppResult.runSuspendCatching("history.insert") {
            sessionManager.query { historyDao().insert(snapshot.toEntity()) }
        }.onFailureLog("HistoryRepo")
    }

    override suspend fun deleteByEntryId(entryId: String) {
        if (sessionState.isLocked()) return
        sessionManager.query { historyDao().deleteByEntryId(entryId) }
    }

    private fun List<VaultSnapshotEntity>.toDomainList(): List<VaultSnapshot> =
        map { it.toDomain() }

    private fun VaultSnapshotEntity.toDomain(): VaultSnapshot = VaultSnapshot(
        snapshotId = historyId,
        entryId = entryId,
        version = version,
        createdAt = createdAt,
        changeType = SnapshotType.fromValue(changeType)
    )

    private fun VaultSnapshot.toEntity(): VaultSnapshotEntity = VaultSnapshotEntity(
        historyId = snapshotId,
        entryId = entryId,
        version = version,
        snapshotBlob = ByteArray(0),
        changeType = changeType.value,
        createdAt = createdAt
    )
}
