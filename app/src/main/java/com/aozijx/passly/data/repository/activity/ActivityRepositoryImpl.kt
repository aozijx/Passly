package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.activity.toDomain
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.repository.activity.CommandActivityRepository
import com.aozijx.passly.domain.repository.activity.QueryActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider
) : QueryActivityRepository, CommandActivityRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityDao().observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityDao().observeAll()
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityDao().observeByType(activityType)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    override suspend fun deleteByEntryId(entryId: String) {
        if (sessionState.isLocked()) return
        sessionManager.transaction { entryActivityDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        if (sessionState.isLocked()) return
        sessionManager.transaction { entryActivityDao().deleteBefore(timestamp) }
    }
}
