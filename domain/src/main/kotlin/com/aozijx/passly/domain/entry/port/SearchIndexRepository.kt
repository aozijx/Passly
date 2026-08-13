package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.query.LookupField
import kotlinx.coroutines.flow.Flow

interface SearchIndexRepository {
    fun search(query: String, fields: List<LookupField> = LookupField.entries): Flow<List<String>>
}
