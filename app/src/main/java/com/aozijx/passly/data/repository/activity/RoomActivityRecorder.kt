package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.repository.activity.ActivityRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomActivityRecorder @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val sessionManager: UnifiedSessionManager,
) : ActivityRecorder {

    override suspend fun recordUsage(
        entryId: String,
        type: ActivityType
    ): AppResult<Unit> = transactionRunner.write("activity.recordUsage") {
        val now = System.currentTimeMillis()
        entryActivityCommandDao().insertIdempotent(
            EntryActivity(entryId = entryId, activityType = type).toEntity(now)
        )
    }

    override suspend fun deleteByEntryId(entryId: String) {
        sessionManager.transaction { entryActivityCommandDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        sessionManager.transaction { entryActivityCommandDao().deleteBefore(timestamp) }
    }

    private companion object {
        private fun EntryActivity.toEntity(now: Long) =
            EntryActivityEntity(
                activityId = activityId,
                entryId = entryId,
                activityType = activityType,
                createdAt = now
            )
    }
}
