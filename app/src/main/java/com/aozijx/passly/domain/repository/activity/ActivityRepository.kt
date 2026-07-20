package com.aozijx.passly.domain.repository.activity

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>>
    fun observeAll(): Flow<List<VaultActivity>>
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivity>>

    suspend fun deleteByEntryId(entryId: String)
    suspend fun deleteBefore(timestamp: Long)
}
