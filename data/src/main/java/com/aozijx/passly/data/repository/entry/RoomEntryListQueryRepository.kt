package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.session.observeWhenReadable
import com.aozijx.passly.data.local.database.query.buildEntryCategoryQuery
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomEntryListQueryRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
) : EntryListQueryRepository {

    override val deletedEntries: Flow<List<EntryListItem>> =
        databaseSession.observeWhenReadable(sessionState.isAuthorized, emptyValue = { emptyList() }) {
            entryQueryDao().observeDeleted()
                .map { entities ->
                    entities.map { entity ->
                        val summary = EntryProfileMapper.fromEntity(entity)
                        EntryListItemMapper.assemble(entity, summary)
                    }
                }
                .flowOn(Dispatchers.IO)
        }

    override val availableCategories: Flow<List<String>> =
        databaseSession.observeWhenReadable(sessionState.isAuthorized, emptyValue = { emptyList() }) {
            entryQueryDao().observeCategories(buildEntryCategoryQuery())
                .flowOn(Dispatchers.IO)
        }

}
