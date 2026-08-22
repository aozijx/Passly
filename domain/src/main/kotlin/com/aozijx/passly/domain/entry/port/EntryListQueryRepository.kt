package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val activeSummaries: Flow<List<EntryListItem>>
    val deletedEntries: Flow<List<EntryListItem>>
}
