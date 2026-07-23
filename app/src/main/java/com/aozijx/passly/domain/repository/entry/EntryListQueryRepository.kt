package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.domain.model.lookup.EntryFilter
import com.aozijx.passly.domain.model.lookup.EntryListItem
import kotlinx.coroutines.flow.Flow

interface EntryListQueryRepository {
    val allCategories: Flow<List<String>>
    fun observe(query: String, category: String?, filter: EntryFilter): Flow<List<EntryListItem>>
    fun observeCategories(filter: EntryFilter): Flow<List<String>>
}
