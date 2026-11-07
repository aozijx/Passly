package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import kotlinx.coroutines.flow.Flow

interface ActivityQueryRepository {
    fun observeByEntryId(entryId: String): Flow<List<EntryActivity>>
}
