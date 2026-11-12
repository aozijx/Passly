package com.aozijx.passly.data.repository.entry.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.aozijx.passly.data.local.database.query.buildEntryPagingQuery
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.port.EntryPagingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomEntryPagingStore
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
    ) : EntryPagingRepository {
        override fun pages(
            query: EntryListQuery,
            config: PagingConfig,
        ): Flow<PagingData<EntryListItem>> =
            databaseSession.observeFullVaultWhenReadable(emptyValue = { PagingData.empty() }) {
                Pager(config = config) {
                    entryQueryDao().paging(buildEntryPagingQuery(query))
                }.flow.map { pagingData ->
                    pagingData.map { row ->
                        EntryListItemMapper.fromEntity(
                            entity = row.entry,
                            usage =
                                EntryUsage(
                                    count = row.usageCount,
                                    lastUsedAtMs = row.lastUsedAt,
                                ),
                            accountId = row.accountId?.let(::EntryId),
                        )
                    }
                }
            }
    }
