package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val deletedEntries: Flow<List<EntryListItem>>
    fun observe(
        query: String,
        filter: EntryFilter
    ): Flow<List<EntryListItem>>
}
