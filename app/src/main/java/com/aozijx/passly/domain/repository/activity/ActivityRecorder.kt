package com.aozijx.passly.domain.repository.activity

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType

interface ActivityRecorder {
    suspend fun recordUsage(
        entryId: String,
        type: ActivityType = ActivityType.VIEW
    ): AppResult<Unit>

    suspend fun deleteByEntryId(entryId: String)
    suspend fun deleteBefore(timestamp: Long)
}
