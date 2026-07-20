package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.activity.ActivityRepository
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
    private val sessionManager: DatabaseSession,
    private val sessionState: VaultAccessState
) : ActivityRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeAll()
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByType(activityType: ActivityType): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeByType(activityType)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    override suspend fun deleteByEntryId(entryId: String) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().deleteBefore(timestamp) }
    }
}
