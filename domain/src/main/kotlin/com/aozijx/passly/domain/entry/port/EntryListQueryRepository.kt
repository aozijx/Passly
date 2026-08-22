package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val deletedEntries: Flow<List<EntryListItem>>
    fun observeSummaries(
        query: String,
        filter: EntryFilter
    ): Flow<List<EntryListItem>>
}
