package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId

interface EntryQueryRepository {
    suspend fun getById(entryId: EntryId): Entry?
    suspend fun findEntriesWithCustomIcons(): List<Entry>
    suspend fun count(): Int
}
