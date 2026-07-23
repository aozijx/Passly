package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.domain.model.entry.VaultEntry

interface EntryQueryRepository {
    suspend fun getById(entryId: String): VaultEntry?
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun count(): Int
}
