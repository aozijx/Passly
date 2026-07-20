package com.aozijx.passly.domain.repository.vault

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>>
    fun observeAll(): Flow<List<VaultActivity>>
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivity>>

    suspend fun getById(activityId: String): VaultActivity?
    suspend fun getByEntryId(entryId: String): List<VaultActivity>

    suspend fun exists(activityId: String): Boolean
    suspend fun count(): Int

    suspend fun insert(activity: VaultActivity)
    suspend fun insertAll(activities: List<VaultActivity>)

    suspend fun deleteByEntryId(entryId: String)
    suspend fun deleteBefore(timestamp: Long)
    suspend fun clear()
}
