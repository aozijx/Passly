package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.local.database.query.buildEntryIdIntersectionQuery
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.data.mapper.entry.databaseFlag
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryUsage
import com.aozijx.passly.domain.entry.model.query.LookupField
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.security.search.BlindIndexer
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
    private val summaryCodec: EntryProfileCodec,
    private val blindIndexer: BlindIndexer
) : EntryListQueryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val deletedEntries: Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryQueryDao().observeDeleted()
                    .map { entities ->
                        entities.map { entity ->
                            val summary = summaryCodec.decrypt(
                                entity.summaryBlob,
                                entity.entryId
                            )
                            EntryListItemMapper.assemble(entity, summary)
                        }
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(
        query: String, filter: EntryFilter
    ): Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else databaseSession.observeFlow {
                val entryFlow = when (filter) {
                    EntryFilter.ALL -> entryQueryDao().observeActive()
                    EntryFilter.TOTP_ONLY -> entryQueryDao().observeActiveWithCapability(
                        databaseFlag(EntryCapability.OTP)
                    )

                    EntryFilter.PASSWORD_ONLY -> entryQueryDao().observeActiveWithCapability(
                        databaseFlag(EntryCapability.PASSWORD)
                    )
                }
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
                    // 使用盲索引预过滤：仅解密匹配的条目
                    val filteredMetaEntities = if (query.isNotEmpty()) {
                        filterByBlindIndex(this, metaEntities, query)
                    } else {
                        metaEntities
                    }

                    filteredMetaEntities.map {
                        val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
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
                        .filter { item ->
                            when (filter) {
                                EntryFilter.ALL -> true
                                EntryFilter.PASSWORD_ONLY -> item.hasPassword
                                EntryFilter.TOTP_ONLY -> item.hasOtp
                            }
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

    /**
     * 使用盲索引对 [metaEntities] 进行预过滤，仅返回与 [query] 匹配的条目。
     */
    private suspend fun filterByBlindIndex(
        db: AppDatabase,
        metaEntities: List<EntryEntity>,
        query: String
    ): List<EntryEntity> {
        val searchTokens = blindIndexer.searchTokens(query)
        if (searchTokens.isEmpty()) return emptyList()

        val sqlQuery = buildEntryIdIntersectionQuery(searchTokens, SEARCH_FIELDS)
        val matchingIds = db.searchTokenQueryDao()
            .searchByTokenIntersection(sqlQuery)
            .toSet()

        if (matchingIds.isEmpty()) return emptyList()

        return metaEntities.filter { it.entryId in matchingIds }
    }

    private companion object {
        private val SEARCH_FIELDS = LookupField.entries
    }
}
