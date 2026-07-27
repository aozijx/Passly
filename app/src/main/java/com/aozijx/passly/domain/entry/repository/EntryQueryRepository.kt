package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.VaultEntry

interface EntryQueryRepository {
    suspend fun getById(entryId: String): VaultEntry?
    suspend fun getEntriesForIconResync(): List<VaultEntry>
    suspend fun count(): Int
}
