package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository.EntryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultSearchRepositoryImpl(
    private val entryDao: VaultEntryDao
) : VaultSearchRepository {

    override val allCategories: Flow<List<String>> = entryDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
            .mapNotNull { it.category.takeIf { c -> c.isNotEmpty() } }
            .distinct()
    }

    override fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: EntryFilter
    ): Flow<List<VaultSummary>> = entryDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
            .filter { entry ->
                (query.isEmpty() || entry.title.contains(query, ignoreCase = true)
                        || entry.username.contains(query, ignoreCase = true)
                        || entry.email?.contains(query, ignoreCase = true) == true
                        || entry.category.contains(query, ignoreCase = true)
                        || entry.tags?.any { it.contains(query, ignoreCase = true) } == true)
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

    override fun getCategoriesByFilter(filter: EntryFilter): Flow<List<String>> =
        entryDao.observeAll().map { entities ->
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
}