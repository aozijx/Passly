package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.activity.ActivityType

interface ActivityRecorder {
    suspend fun recordUsage(
        entryId: String,
        type: ActivityType = ActivityType.VIEW
    ): AppResult<Unit>

    suspend fun deleteByEntryId(entryId: String)
    suspend fun deleteBefore(timestamp: Long)
}
