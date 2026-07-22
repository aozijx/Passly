package com.aozijx.passly.data.repository.lookup

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.domain.repository.lookup.LookupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    private val stateProvider: SessionStateProvider,
    private val cryptoMapper: VaultEntryCryptoMapper
) : LookupRepository {

    private companion object {
        /**
         * TOTP 不是独立 [EntryType]，而是 [VaultEntry.credential.otp] 中的可选配置。
         * 以下列表用于 SQL 层面的粗筛（优化性能），实际精确匹配由解密后的二次过滤层完成。
         */
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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategories: Flow<List<String>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                metadataDao().observeActive()
                    .map { metaEntities ->
                        val credEntities =
                            credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull {
                            cryptoMapper.assembleListItem(
                                it,
                                credMap[it.entryId]
                            )
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
    ): Flow<List<VaultListItem>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.observeFlow {
                val entryFlow = when (filter) {
                    LookupRepository.EntryFilter.ALL -> metadataDao().observeActive()
                    LookupRepository.EntryFilter.TOTP_ONLY -> metadataDao().observeByEntryTypes(
                        TOTP_ENTRY_TYPES
                    )

                    LookupRepository.EntryFilter.PASSWORD_ONLY -> metadataDao().observeByEntryTypes(
                        PASSWORD_ENTRY_TYPES
                    )
                }
                entryFlow
                    .map { metaEntities ->
                        val credEntities =
                            credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull {
                            cryptoMapper.assembleListItem(
                                it,
                                credMap[it.entryId]
                            )
                        }
                            .filter { item ->
                                ((query.isEmpty() || item.title.contains(
                                    query,
                                    ignoreCase = true
                                )
                                        || item.username.contains(query, ignoreCase = true)
                                        || item.category.contains(
                                    query,
                                    ignoreCase = true
                                )) || item.tags.any {
                                    it.contains(
                                        query,
                                        ignoreCase = true
                                    )
                                })
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
                                compareByDescending<VaultListItem> { it.favorite }
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
                        LookupRepository.EntryFilter.ALL -> metadataDao().observeActive()
                        LookupRepository.EntryFilter.TOTP_ONLY -> metadataDao().observeByEntryTypes(
                            TOTP_ENTRY_TYPES
                        )

                        LookupRepository.EntryFilter.PASSWORD_ONLY -> metadataDao().observeByEntryTypes(
                            PASSWORD_ENTRY_TYPES
                        )
                    }
                    entryFlow
                        .map { metaEntities ->
                            val credEntities =
                                credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                            val credMap = credEntities.associateBy { it.entryId }
                            metaEntities.mapNotNull {
                                cryptoMapper.assembleListItem(
                                    it,
                                    credMap[it.entryId]
                                )
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
}
