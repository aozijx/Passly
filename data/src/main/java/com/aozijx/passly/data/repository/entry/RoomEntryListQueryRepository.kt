package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override val deletedEntries: Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
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
    override val activeSummaries: Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else databaseSession.observeFlow {
                val entryFlow = entryQueryDao().observeActive()
                val statsFlow =
                    entryActivityAnalyticsDao().observeUsageStats(ActivityType.USAGE_TYPES)
                val linksFlow = entryLinkQueryDao().observeAll()
                combine(entryFlow, statsFlow, linksFlow) { metaEntities, statsList, linkEntities ->
                    val statsMap = statsList.associateBy { it.entryId }
                    val accountGraph = EntryAccountGraph(
                        linkEntities.map { link ->
                            com.aozijx.passly.domain.entry.model.relation.EntryLink.create(
                                id = com.aozijx.passly.domain.entry.model.relation.EntryLinkId(link.linkId),
                                sourceEntryId = EntryId(link.sourceEntryId),
                                targetEntryId = EntryId(link.targetEntryId),
                                relationType = link.relationType,
                                createdAt = link.createdAt,
                                updatedAt = link.updatedAt
                            )
                        }
                    )
                    metaEntities.map {
                        val summary = EntryProfileMapper.fromEntity(it)
                        EntryListItemMapper.assemble(it, summary)
                    }
                        .map { item ->
                            val stats = statsMap[item.id.value]
                            val groupedItem = item.copy(
                                accountId = accountGraph.accountFor(item.id)
                            )
                            if (stats != null) groupedItem.copy(
                                usage = EntryUsage(
                                    count = stats.usageCount,
                                    lastUsedAtMs = stats.lastUsedAt,
                                )
                            ) else groupedItem
                        }
                        .sortedWith(
                            compareByDescending<EntryListItem> { it.favorite }
                                .thenByDescending { it.usageCount }
                                .thenByDescending { it.createdAt }
                        )
                }
                    .flowOn(Dispatchers.IO)
            }
        }

}
