package com.aozijx.passly.data.repository.lookup

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
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
    private val cryptoMapper: VaultEntryCryptoMapper
) : LookupRepository {

    private companion object {
        private val TOTP_ENTRY_TYPES = emptyList<EntryType>()
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
            else sessionManager.read {
                metadataDao().observeActive()
                    .map { metaEntities ->
                        val credEntities =
                            credentialDao().getByEntryIds(metaEntities.map { it.entryId })
                        val credMap = credEntities.associateBy { it.entryId }
                        metaEntities.mapNotNull {
                            cryptoMapper.assembleEntry(
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
    ): Flow<List<VaultEntry>> = sessionState.isAuthorized
        .flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.read {
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
                            cryptoMapper.assembleEntry(
                                it,
                                credMap[it.entryId]
                            )
                        }
                            .filter { entry ->
                                ((query.isEmpty() || entry.title.contains(
                                    query,
                                    ignoreCase = true
                                )
                                        || entry.username.contains(query, ignoreCase = true)
                                        || entry.credential.email?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true
                                        || entry.category.contains(
                                    query,
                                    ignoreCase = true
                                )) || entry.tags.any {
                                    it.contains(
                                        query,
                                        ignoreCase = true
                                    )
                                })
                            }
                            .filter { entry ->
                                category == null || entry.category == category
                            }
                            .filter { entry ->
                                when (filter) {
                                    LookupRepository.EntryFilter.ALL -> true
                                    LookupRepository.EntryFilter.PASSWORD_ONLY -> entry.credential.otp?.secret.isNullOrEmpty()
                                    LookupRepository.EntryFilter.TOTP_ONLY -> !entry.credential.otp?.secret.isNullOrEmpty()
                                }
                            }
                            .sortedWith(
                                compareByDescending<VaultEntry> { it.favorite }
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
                else sessionManager.read {
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
                                cryptoMapper.assembleEntry(
                                    it,
                                    credMap[it.entryId]
                                )
                            }
                                .filter { entry ->
                                    when (filter) {
                                        LookupRepository.EntryFilter.ALL -> true
                                        LookupRepository.EntryFilter.PASSWORD_ONLY -> entry.credential.otp?.secret.isNullOrEmpty()
                                        LookupRepository.EntryFilter.TOTP_ONLY -> !entry.credential.otp?.secret.isNullOrEmpty()
                                    }
                                }
                                .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                                .distinct()
                        }
                        .flowOn(Dispatchers.IO)
                }
            }
}
