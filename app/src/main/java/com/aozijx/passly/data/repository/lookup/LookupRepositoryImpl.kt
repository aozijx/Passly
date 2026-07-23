package com.aozijx.passly.data.repository.lookup

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.dao.buildEntryIdIntersectionQuery
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.entry.EntryListItemMapper
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.lookup.LookupField
import com.aozijx.passly.domain.repository.lookup.LookupRepository
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
class LookupRepositoryImpl @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val blindIndexer: BlindIndexer
) : LookupRepository {

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

        /** 搜索覆盖的所有字段 */
        private val SEARCH_FIELDS = LookupField.entries
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategories: Flow<List<String>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                entryDao().observeActive()
                    .map { metaEntities ->
                        val credEntities =
                            entrySecretDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull {
                            val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
                            val secret = credMap[it.entryId]?.let { e ->
                                secretCodec.decrypt(
                                    e.secretBlob,
                                    e.entryId
                                )
                            }
                            EntryListItemMapper.assemble(it, summary, secret)
                        }
                            .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                            .distinct()
                    }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(
        query: String, category: String?, filter: LookupRepository.EntryFilter
    ): Flow<List<EntryListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                val entryFlow = when (filter) {
                    LookupRepository.EntryFilter.ALL -> entryDao().observeActive()
                    LookupRepository.EntryFilter.TOTP_ONLY -> entryDao().observeActiveByTypes(
                        TOTP_ENTRY_TYPES
                    )

                    LookupRepository.EntryFilter.PASSWORD_ONLY -> entryDao().observeActiveByTypes(
                        PASSWORD_ENTRY_TYPES
                    )
                }
                val statsFlow = entryActivityDao().observeUsageStats(ActivityType.USAGE_TYPES)
                combine(entryFlow, statsFlow) { metaEntities, statsList ->
                    val statsMap = statsList.associateBy { it.entryId }
                    // 使用盲索引预过滤：仅解密匹配的条目
                    val filteredMetaEntities = if (query.isNotEmpty()) {
                        filterByBlindIndex(this, metaEntities, query)
                    } else {
                        metaEntities
                    }

                    val credEntities =
                        entrySecretDao().getByEntryIds(filteredMetaEntities.map { it.entryId })
                    val credMap = credEntities.associateBy { it.entryId }
                    filteredMetaEntities.mapNotNull {
                        val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
                        val secret = credMap[it.entryId]?.let { e ->
                            secretCodec.decrypt(
                                e.secretBlob,
                                e.entryId
                            )
                        }
                        EntryListItemMapper.assemble(it, summary, secret)
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
                                LookupRepository.EntryFilter.ALL -> true
                                LookupRepository.EntryFilter.PASSWORD_ONLY -> !item.hasTotp
                                LookupRepository.EntryFilter.TOTP_ONLY -> item.hasTotp
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
    override fun observeCategories(filter: LookupRepository.EntryFilter): Flow<List<String>> =
        sessionState.isAuthorized
            .flatMapLatest { authorized ->
                if (!authorized) flowOf(emptyList())
                else sessionManager.observeFlow {
                    val entryFlow = when (filter) {
                        LookupRepository.EntryFilter.ALL -> entryDao().observeActive()
                        LookupRepository.EntryFilter.TOTP_ONLY -> entryDao().observeActiveByTypes(
                            TOTP_ENTRY_TYPES
                        )

                        LookupRepository.EntryFilter.PASSWORD_ONLY -> entryDao().observeActiveByTypes(
                            PASSWORD_ENTRY_TYPES
                        )
                    }
                    entryFlow
                        .map { metaEntities ->
                            val credEntities =
                                entrySecretDao().getByEntryIds(metaEntities.map { it.entryId })
                            val credMap = credEntities.associateBy { it.entryId }
                            metaEntities.mapNotNull {
                                val summary = summaryCodec.decrypt(it.summaryBlob, it.entryId)
                                val secret = credMap[it.entryId]?.let { e ->
                                    secretCodec.decrypt(
                                        e.secretBlob,
                                        e.entryId
                                    )
                                }
                                EntryListItemMapper.assemble(it, summary, secret)
                            }
                                .filter { item ->
                                    when (filter) {
                                        LookupRepository.EntryFilter.ALL -> true
                                        LookupRepository.EntryFilter.PASSWORD_ONLY -> !item.hasTotp
                                        LookupRepository.EntryFilter.TOTP_ONLY -> item.hasTotp
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
     * 如果查询词无法生成有效令牌（如长度不足），返回空列表。
     *
     * 多 Token 交集在 SQL 层通过 INTERSECT 完成，避免在内存中加载大量候选结果。
     */
    private suspend fun filterByBlindIndex(
        db: AppDatabase,
        metaEntities: List<EntryEntity>,
        query: String
    ): List<EntryEntity> {
        val searchTokens = blindIndexer.searchTokens(query)
        if (searchTokens.isEmpty()) return emptyList()

        val sqlQuery = buildEntryIdIntersectionQuery(searchTokens, SEARCH_FIELDS)
        val matchingIds = db.searchTokenDao()
            .searchByTokenIntersection(sqlQuery)
            .toSet()

        if (matchingIds.isEmpty()) return emptyList()

        return metaEntities.filter { it.entryId in matchingIds }
    }
}
