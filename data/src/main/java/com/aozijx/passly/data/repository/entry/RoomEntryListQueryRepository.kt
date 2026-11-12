package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.query.buildEntryCategoryQuery
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomEntryListQueryRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
    ) : EntryListQueryRepository {
        override suspend fun getActiveByType(entryType: EntryType) =
            databaseSession.queryFullVault(
                onDenied = { emptyList() },
            ) {
                entryQueryDao().getActiveByType(entryType).map { EntryListItemMapper.fromEntity(it) }
            }

        override val deletedEntries: Flow<List<EntryListItem>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { emptyList() }) {
                entryQueryDao()
                    .observeDeleted()
                    .map { entities ->
                        entities.map { EntryListItemMapper.fromEntity(it) }
                    }.flowOn(Dispatchers.IO)
            }

        override val availableCategories: Flow<List<String>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { emptyList() }) {
                entryQueryDao()
                    .observeCategories(buildEntryCategoryQuery())
                    .flowOn(Dispatchers.IO)
            }
    }
