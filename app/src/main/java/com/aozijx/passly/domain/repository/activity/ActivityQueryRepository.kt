package com.aozijx.passly.domain.repository.activity

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import kotlinx.coroutines.flow.Flow

interface ActivityQueryRepository {
    fun observeByEntryId(entryId: String): Flow<List<EntryActivity>>
    fun observeAll(): Flow<List<EntryActivity>>
    fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>>
}
