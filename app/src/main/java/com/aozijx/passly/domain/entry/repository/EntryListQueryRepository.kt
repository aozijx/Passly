package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val allEntryTypes: Flow<List<String>>
    val deletedEntries: Flow<List<EntryListItem>>
    fun observe(
        query: String,
        entryTypeName: String?,
        filter: EntryFilter
    ): Flow<List<EntryListItem>>
    fun observeEntryTypes(filter: EntryFilter): Flow<List<String>>
}
