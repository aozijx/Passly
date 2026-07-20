package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.vault.ActivityRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityUseCases @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val vaultRepository: VaultRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        activityRepository.observeByEntryId(entryId)

    fun observeAll(): Flow<List<VaultActivity>> =
        activityRepository.observeAll()

    suspend fun getByEntryId(entryId: String): List<VaultActivity> =
        activityRepository.getByEntryId(entryId)

    suspend fun insert(activity: VaultActivity) =
        activityRepository.insert(activity)

    /**
     * 记录活动并递增条目使用次数。
     * 协调 ActivityRepository 和 VaultRepository，两者职责独立。
     */
    suspend fun recordUsage(
        entryId: String,
        activityType: ActivityType = ActivityType.VIEW
    ): AppResult<Unit> {
        val activity = VaultActivity(
            entryId = entryId,
            activityType = activityType
        )
        activityRepository.insert(activity)
        return vaultRepository.incrementUsage(entryId)
    }

    suspend fun deleteByEntryId(entryId: String) =
        activityRepository.deleteByEntryId(entryId)

    suspend fun deleteBefore(timestamp: Long) =
        activityRepository.deleteBefore(timestamp)

    suspend fun clear() = activityRepository.clear()
}
