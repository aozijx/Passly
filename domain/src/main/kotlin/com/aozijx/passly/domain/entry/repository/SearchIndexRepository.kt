package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.lookup.LookupField
import kotlinx.coroutines.flow.Flow

interface SearchIndexRepository {
    fun search(query: String, fields: List<LookupField> = LookupField.entries): Flow<List<String>>
}
