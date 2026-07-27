package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.activity.toDomain
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.repository.ActivityQueryRepository
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
class RoomActivityQueryRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState
) : ActivityQueryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityQueryDao().observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityQueryDao().observeAll()
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryActivityQueryDao().observeByType(activityType)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }
}
