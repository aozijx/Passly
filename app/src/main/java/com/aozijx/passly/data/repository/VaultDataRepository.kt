package com.aozijx.passly.data.repository

import com.aozijx.passly.data.local.VaultDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultDataRepository(private val dao: VaultDao) : VaultRepository {
    override val allEntries: Flow<List<VaultEntry>> = dao.getAllEntries().map { entities ->
        entities.map { it.toDomain() }
    }
    override val allEntrySummaries: Flow<List<VaultSummary>> = dao.getAllEntrySummaries()

    override val allCategories: Flow<List<String>> = dao.getAllCategories()

    override fun getEntriesByCategory(category: String): Flow<List<VaultEntry>> = 
        dao.getEntriesByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getEntrySummariesByCategory(category: String): Flow<List<VaultSummary>> =
        dao.getEntrySummariesByCategory(category)

    override fun searchEntries(query: String): Flow<List<VaultEntry>> = 
        dao.searchEntries(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchEntrySummaries(query: String): Flow<List<VaultSummary>> =
        dao.searchEntrySummaries(query)

    override fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>> = 
        dao.getHistoryByEntryId(entryId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getEntryById(entryId: Int): VaultEntry? = 
        dao.getEntryById(entryId)?.toDomain()

    override suspend fun insert(entry: VaultEntry): Long = 
        dao.insert(entry.toEntity())

    override suspend fun update(entry: VaultEntry) = 
        dao.update(entry.toEntity())

    override suspend fun delete(entry: VaultEntry) = 
        dao.delete(entry.toEntity())

    override suspend fun deleteAll() = 
        dao.deleteAll()
}
