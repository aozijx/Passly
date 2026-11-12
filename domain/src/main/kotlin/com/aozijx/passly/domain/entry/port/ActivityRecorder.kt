package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType

interface ActivityRecorder {
    suspend fun recordUsage(
        entryId: String,
        type: ActivityType = ActivityType.VIEW,
        affectedFields: Set<FieldKey> = emptySet(),
    ): AppResult<Unit>

    suspend fun deleteByEntryId(entryId: String)

    suspend fun deleteBefore(timestamp: Long)
}
