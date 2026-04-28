package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultDataRepository(
    private val entryDao: VaultEntryDao
) : VaultRepository {
    override val allEntries: Flow<List<VaultEntry>> = entryDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeByType(type: EntryType): Flow<List<VaultEntry>> =
        entryDao.observeByType(type.value).map { it.toDomainList() }

    override suspend fun getEntryById(entryId: Int): VaultEntry? =
        entryDao.getEntryById(entryId)?.toDomain()

    override suspend fun getByType(type: EntryType): List<VaultEntry> =
        entryDao.getByType(type.value).toDomainList()

    override suspend fun getEntriesForIconResync(): List<VaultEntry> =
        entryDao.getAll().toDomainList().filter {
            !it.associatedDomain.isNullOrEmpty()
        }

    override suspend fun count(): Int = entryDao.count()

    override suspend fun countByType(type: EntryType): Int = entryDao.countByType(type.value)

    override suspend fun insert(entry: VaultEntry): Long =
        entryDao.insert(entry.toEntity())

    override suspend fun update(entry: VaultEntry) =
        entryDao.update(entry.toEntity())

    override suspend fun delete(entry: VaultEntry) =
        entryDao.delete(entry.toEntity())

    override suspend fun deleteAll() =
        entryDao.deleteAll()
}
