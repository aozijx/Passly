package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.session.observeDatabaseReadAccess
import com.aozijx.passly.data.mapper.activity.toDomain
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
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
internal class RoomActivityQueryRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState
) : ActivityQueryRepository {

    private val canRead = observeDatabaseReadAccess(sessionState.isAuthorized, databaseSession.lockStateFlow)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
        canRead.flatMapLatest { readable ->
            if (!readable) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryActivityQueryDao().observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<EntryActivity>> =
        canRead.flatMapLatest { readable ->
            if (!readable) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryActivityQueryDao().observeAll()
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> =
        canRead.flatMapLatest { readable ->
            if (!readable) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryActivityQueryDao().observeByType(activityType)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }
}
