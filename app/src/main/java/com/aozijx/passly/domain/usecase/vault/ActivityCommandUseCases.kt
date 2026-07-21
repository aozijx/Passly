package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.repository.activity.CommandActivityRepository
import com.aozijx.passly.domain.repository.entry.RecordEntryUsageFacade
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityCommandUseCases @Inject constructor(
    private val commandActivityRepository: CommandActivityRepository,
    private val recordEntryUsageFacade: RecordEntryUsageFacade
) {
    suspend fun recordUsage(
        entryId: String,
        activityType: ActivityType = ActivityType.VIEW
    ): AppResult<Unit> =
        recordEntryUsageFacade.record(entryId, activityType)

    suspend fun deleteByEntryId(entryId: String) =
        commandActivityRepository.deleteByEntryId(entryId)

    suspend fun deleteBefore(timestamp: Long) =
        commandActivityRepository.deleteBefore(timestamp)
}
