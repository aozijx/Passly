package com.aozijx.passly.domain.repository.search

import com.aozijx.passly.domain.model.lookup.LookupField
import kotlinx.coroutines.flow.Flow

interface SearchIndexRepository {
    fun search(query: String, fields: List<LookupField> = LookupField.entries): Flow<List<String>>
}
