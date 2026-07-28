package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val allCategories: Flow<List<String>>
    val deletedEntries: Flow<List<EntryListItem>>
    fun observe(query: String, category: String?, filter: EntryFilter): Flow<List<EntryListItem>>
    fun observeCategories(filter: EntryFilter): Flow<List<String>>
}
