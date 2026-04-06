package com.aozijx.passly.data.repository

import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultSearchDataRepository(
    private val entryDao: VaultEntryDao
) : VaultSearchRepository {
    override val allEntrySummaries: Flow<List<VaultSummary>> = entryDao.getAllEntrySummaries()

    override val allCategories: Flow<List<String>> = entryDao.getAllCategories()

    override fun getEntriesByCategory(category: String): Flow<List<VaultEntry>> = 
        entryDao.getEntriesByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getEntrySummariesByCategory(category: String): Flow<List<VaultSummary>> =
        entryDao.getEntrySummariesByCategory(category)

    override fun searchEntries(query: String): Flow<List<VaultEntry>> = 
        entryDao.searchEntries(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchEntrySummaries(query: String): Flow<List<VaultSummary>> =
        entryDao.searchEntrySummaries(query)
}
