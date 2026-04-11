package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultDataRepository(
    private val entryDao: VaultEntryDao
) : VaultRepository {
    override val allEntries: Flow<List<VaultEntry>> = entryDao.getAllEntries().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getEntryById(entryId: Int): VaultEntry? = 
        entryDao.getEntryById(entryId)?.toDomain()

    override suspend fun insert(entry: VaultEntry): Long = 
        entryDao.insert(entry.toEntity())

    override suspend fun update(entry: VaultEntry) = 
        entryDao.update(entry.toEntity())

    override suspend fun delete(entry: VaultEntry) = 
        entryDao.delete(entry.toEntity())

    override suspend fun deleteAll() = 
        entryDao.deleteAll()
}