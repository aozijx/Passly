package com.aozijx.passly.data.repository

import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow

class VaultSearchDataRepository(
    private val entryDao: VaultEntryDao
) : VaultSearchRepository {
    override val allCategories: Flow<List<String>> = entryDao.getAllCategories()

    override fun observeEntrySummariesByDemand(
        query: String, category: String?, filter: VaultSearchRepository.EntryFilter
    ): Flow<List<VaultSummary>> =
        entryDao.observeEntrySummariesByDemand(query, category, filter.ordinal)

    override fun getCategoriesByFilter(filter: VaultSearchRepository.EntryFilter): Flow<List<String>> =
        entryDao.getCategoriesByFilter(filter.ordinal)
}
