package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.EntryAggregate

interface EntryQueryRepository {
    suspend fun getByIdWithoutHighSensitivity(entryId: String): EntryAggregate?
    suspend fun getEntriesForIconResync(): List<EntryAggregate>
    suspend fun count(): Int
}
