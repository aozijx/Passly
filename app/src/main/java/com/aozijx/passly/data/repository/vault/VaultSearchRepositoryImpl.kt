package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository.EntryFilter
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import com.aozijx.passly.security.crypto.LockState
import com.aozijx.passly.security.crypto.VaultLockManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSearchRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSessionManager,
    private val lockManager: VaultLockManager
) : VaultSearchRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allCategories: Flow<List<String>> = lockManager.lockState
        .flatMapLatest { state ->
            if (state == LockState.LOCKED) flowOf(emptyList())
            else channelFlow {
                sessionManager.withDatabase {
                    vaultEntryDao().observeAll()
                        .map { entities ->
                            entities.map { it.toDomain() }
                                .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                                .distinct()
                        }
                        .collect { send(it) }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<VaultSummary>> = lockManager.lockState
        .flatMapLatest { state ->
            if (state == LockState.LOCKED) flowOf(emptyList())
            else channelFlow {
                sessionManager.withDatabase {
                    vaultEntryDao().observeAll()
                        .map { entities ->
                            entities.map { it.toDomain() }
                                .filter { entry ->
                                    (query.isEmpty() || entry.title.contains(
                                        query,
                                        ignoreCase = true
                                    )
                                            || entry.username.contains(query, ignoreCase = true)
                                            || entry.email?.contains(
                                        query,
                                        ignoreCase = true
                                    ) == true
                                            || entry.category.contains(query, ignoreCase = true)
                                            || entry.tags?.any {
                                        it.contains(
                                            query,
                                            ignoreCase = true
                                        )
                                    } == true)
                                }
                                .filter { entry ->
                                    category == null || entry.category == category
                                }
                                .filter { entry ->
                                    when (filter) {
                                        EntryFilter.ALL -> true
                                        EntryFilter.PASSWORD_ONLY -> entry.totpSecret.isNullOrEmpty()
                                        EntryFilter.TOTP_ONLY -> !entry.totpSecret.isNullOrEmpty()
                                    }
                                }
                                .sortedWith(
                                    compareByDescending<com.aozijx.passly.domain.model.VaultEntry> { it.favorite }
                                        .thenByDescending { it.usageCount }
                                        .thenByDescending { it.createdAt ?: 0L }
                                )
                                .map { it.toSummary() }
                        }
                        .collect { send(it) }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCategoriesByFilter(filter: EntryFilter): Flow<List<String>> =
        lockManager.lockState
            .flatMapLatest { state ->
                if (state == LockState.LOCKED) flowOf(emptyList())
                else channelFlow {
                    sessionManager.withDatabase {
                        vaultEntryDao().observeAll()
                            .map { entities ->
                                entities.map { it.toDomain() }
                                    .filter { entry ->
                                        when (filter) {
                                            EntryFilter.ALL -> true
                                            EntryFilter.PASSWORD_ONLY -> entry.totpSecret.isNullOrEmpty()
                                            EntryFilter.TOTP_ONLY -> !entry.totpSecret.isNullOrEmpty()
                                        }
                                    }
                                    .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
                                    .distinct()
                            }
                            .collect { send(it) }
                    }
                }
            }
}