package com.aozijx.passly.data.repository.entry.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.aozijx.passly.data.local.database.query.buildEntryPagingQuery
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.runtime.session.SecureSessionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomEntryPagingStore @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
) : EntryPagingStore {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun pages(
        query: EntryListQuery,
        config: PagingConfig,
    ): Flow<PagingData<EntryListItem>> = combine(
        sessionState.isAuthorized,
        databaseSession.lockStateFlow,
    ) { authorized, databaseState ->
        authorized && databaseState == SecureSessionState.UNLOCKED
    }
        .distinctUntilChanged()
        .flatMapLatest { canRead ->
            if (!canRead) {
                flowOf(PagingData.empty())
            } else {
                databaseSession.observeFlow {
                    Pager(config = config) {
                        entryQueryDao().paging(buildEntryPagingQuery(query))
                    }.flow.map { pagingData ->
                        pagingData.map { row ->
                            val item = EntryListItemMapper.assemble(
                                entity = row.entry,
                                summary = EntryProfileMapper.fromEntity(row.entry),
                            )
                            item.copy(
                                usage = EntryUsage(
                                    count = row.usageCount,
                                    lastUsedAtMs = row.lastUsedAt,
                                ),
                                accountId = row.accountId?.let(::EntryId),
                            )
                        }
                    }
                }
            }
        }
}
