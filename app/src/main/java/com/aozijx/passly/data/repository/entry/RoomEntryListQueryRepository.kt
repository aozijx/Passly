package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.dao.buildEntryIdIntersectionQuery
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.lookup.LookupField
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
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
class RoomEntryListQueryRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val blindIndexer: BlindIndexer
) : EntryListQueryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val deletedEntries: Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
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
            else sessionManager.observeFlow {
                val entryFlow = when (filter) {
                    EntryFilter.ALL -> entryQueryDao().observeActive()
                    EntryFilter.TOTP_ONLY -> entryQueryDao().observeActiveWithCapability(
                        EntryCapabilityFlags.HAS_OTP
                    )

                    EntryFilter.PASSWORD_ONLY -> entryQueryDao().observeActiveWithCapability(
                        EntryCapabilityFlags.HAS_PASSWORD
                    )
                }
                val statsFlow =
                    entryActivityAnalyticsDao().observeUsageStats(ActivityType.USAGE_TYPES)
                combine(entryFlow, statsFlow) { metaEntities, statsList ->
                    val statsMap = statsList.associateBy { it.entryId }
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
                            val stats = statsMap[item.id]
                            if (stats != null) item.copy(
                                usageCount = stats.usageCount,
                                lastUsedAt = stats.lastUsedAt
                            ) else item
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
