package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.dao.buildEntryIdIntersectionQuery
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.lookup.EntryFilter
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.lookup.LookupField
import com.aozijx.passly.domain.repository.entry.EntryListQueryRepository
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
    private val sessionState: VaultAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val blindIndexer: BlindIndexer
) : EntryListQueryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategories: Flow<List<String>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryQueryDao().observeActive()
                    .map { entities ->
                        entities.map {
                            val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
                            EntryListItemMapper.assemble(it, summary)
                        }
                            .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                            .distinct()
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                val entryFlow = when (filter) {
                    EntryFilter.ALL -> entryQueryDao().observeActive()
                    EntryFilter.TOTP_ONLY -> entryQueryDao().observeActiveByTypes(
                        TOTP_ENTRY_TYPES
                    )

                    EntryFilter.PASSWORD_ONLY -> entryQueryDao().observeActiveByTypes(
                        PASSWORD_ENTRY_TYPES
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
                            category == null || item.category == category
                        }
                        .filter { item ->
                            when (filter) {
                                EntryFilter.ALL -> true
                                EntryFilter.PASSWORD_ONLY -> !item.hasTotp
                                EntryFilter.TOTP_ONLY -> item.hasTotp
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCategories(filter: EntryFilter): Flow<List<String>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) flowOf(emptyList())
                else sessionManager.observeFlow {
                    val entryFlow = when (filter) {
                        EntryFilter.ALL -> entryQueryDao().observeActive()
                        EntryFilter.TOTP_ONLY -> entryQueryDao().observeActiveByTypes(
                            TOTP_ENTRY_TYPES
                        )

                        EntryFilter.PASSWORD_ONLY -> entryQueryDao().observeActiveByTypes(
                            PASSWORD_ENTRY_TYPES
                        )
                    }
                    entryFlow
                        .map { entities ->
                            entities.map {
                                val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
                                EntryListItemMapper.assemble(it, summary)
                            }
                                .filter { item ->
                                    when (filter) {
                                        EntryFilter.ALL -> true
                                        EntryFilter.PASSWORD_ONLY -> !item.hasTotp
                                        EntryFilter.TOTP_ONLY -> item.hasTotp
                                    }
                                }
                                .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                                .distinct()
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
        private val TOTP_ENTRY_TYPES = listOf(
            EntryType.LOGIN,
            EntryType.CARD,
            EntryType.IDENTITY,
            EntryType.NOTE,
            EntryType.WIFI,
            EntryType.SSH_KEY,
            EntryType.CRYPTO_WALLET
        )

        private val PASSWORD_ENTRY_TYPES = listOf(
            EntryType.LOGIN,
            EntryType.CARD,
            EntryType.IDENTITY,
            EntryType.NOTE,
            EntryType.WIFI,
            EntryType.SSH_KEY,
            EntryType.CRYPTO_WALLET
        )

        private val SEARCH_FIELDS = LookupField.entries
    }
}
