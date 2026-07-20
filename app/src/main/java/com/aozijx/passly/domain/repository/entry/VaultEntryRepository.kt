package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.VaultEntry

interface VaultEntryRepository {
    suspend fun getById(entryId: String): VaultEntry?
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun insert(entry: VaultEntry): AppResult<Long>
    suspend fun update(entry: VaultEntry): AppResult<Unit>
    suspend fun delete(entry: VaultEntry): AppResult<Unit>
    suspend fun count(): Int
}
