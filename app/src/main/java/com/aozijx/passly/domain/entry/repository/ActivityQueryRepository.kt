package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import kotlinx.coroutines.flow.Flow

interface ActivityQueryRepository {
    fun observeByEntryId(entryId: String): Flow<List<EntryActivity>>
    fun observeAll(): Flow<List<EntryActivity>>
    fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>>
}
