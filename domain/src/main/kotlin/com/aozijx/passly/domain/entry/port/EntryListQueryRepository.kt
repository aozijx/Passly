package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val availableCategories: Flow<List<String>>
    val deletedEntries: Flow<List<EntryListItem>>

    suspend fun getActiveByType(entryType: EntryType): List<EntryListItem>
}
