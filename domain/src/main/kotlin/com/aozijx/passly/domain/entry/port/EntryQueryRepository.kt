package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType

interface EntryQueryRepository {
    suspend fun getById(entryId: EntryId): Entry?

    suspend fun getByIds(entryIds: List<EntryId>): List<Entry>

    suspend fun getTypesByIds(entryIds: List<EntryId>): Map<EntryId, EntryType>
}
