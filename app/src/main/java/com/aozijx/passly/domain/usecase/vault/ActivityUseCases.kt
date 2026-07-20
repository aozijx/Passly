package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.activity.ActivityRepository
import com.aozijx.passly.domain.repository.entry.RecordEntryUsageFacade
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityUseCases @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val recordEntryUsageFacade: RecordEntryUsageFacade
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        activityRepository.observeByEntryId(entryId)

    fun observeAll(): Flow<List<VaultActivity>> =
        activityRepository.observeAll()

    suspend fun recordUsage(
        entryId: String,
        activityType: ActivityType = ActivityType.VIEW
    ): AppResult<Unit> =
        recordEntryUsageFacade.record(entryId, activityType)

    suspend fun deleteByEntryId(entryId: String) =
        activityRepository.deleteByEntryId(entryId)

    suspend fun deleteBefore(timestamp: Long) =
        activityRepository.deleteBefore(timestamp)
}
