package com.aozijx.passly.domain.repository

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    val allEntries: Flow<List<VaultEntry>>
    val allCategories: Flow<List<String>>

    fun getEntriesByCategory(category: String): Flow<List<VaultEntry>>
    fun searchEntries(query: String): Flow<List<VaultEntry>>
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>>
    suspend fun getEntryById(entryId: Int): VaultEntry?

    suspend fun insert(entry: VaultEntry): Long
    suspend fun update(entry: VaultEntry)
    suspend fun delete(entry: VaultEntry)
    suspend fun deleteAll()
}
