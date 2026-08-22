package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.query.buildEntryCategoryQuery
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.runtime.session.SecureSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomEntryListQueryRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
) : EntryListQueryRepository {

    private val canRead = combine(
        sessionState.isAuthorized,
        databaseSession.lockStateFlow,
    ) { authorized, databaseState ->
        authorized && databaseState == SecureSessionState.UNLOCKED
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val deletedEntries: Flow<List<EntryListItem>> = canRead
        .flatMapLatest { readable ->
            if (!readable) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryQueryDao().observeDeleted()
                    .map { entities ->
                        entities.map { entity ->
                            val summary = EntryProfileMapper.fromEntity(entity)
                            EntryListItemMapper.assemble(entity, summary)
                        }
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val availableCategories: Flow<List<String>> = canRead
        .flatMapLatest { readable ->
            if (!readable) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryQueryDao().observeCategories(buildEntryCategoryQuery())
                    .flowOn(Dispatchers.IO)
            }
        }

}
