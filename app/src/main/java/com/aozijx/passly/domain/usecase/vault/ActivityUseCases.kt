package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.vault.ActivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityUseCases @Inject constructor(
    private val activityRepository: ActivityRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        activityRepository.observeByEntryId(entryId)

    fun observeAll(): Flow<List<VaultActivity>> =
        activityRepository.observeAll()

    suspend fun getByEntryId(entryId: String): List<VaultActivity> =
        activityRepository.getByEntryId(entryId)

    suspend fun insert(activity: VaultActivity) =
        activityRepository.insert(activity)

    suspend fun recordUsage(entryId: String, type: ActivityType = ActivityType.VIEW) =
        activityRepository.record(entryId, type)

    suspend fun deleteByEntryId(entryId: String) =
        activityRepository.deleteByEntryId(entryId)

    suspend fun deleteBefore(timestamp: Long) =
        activityRepository.deleteBefore(timestamp)

    suspend fun clear() = activityRepository.clear()
}
