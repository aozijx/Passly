package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.session.UnifiedSessionManager
import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.repository.ActivityRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomActivityRecorder @Inject constructor(
    private val transactionRunner: VaultTransactionRunner,
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
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
        requireFullSecureSessionAccess("activity.deleteByEntryId")
        sessionManager.transaction { entryActivityCommandDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        requireFullSecureSessionAccess("activity.deleteBefore")
        sessionManager.transaction { entryActivityCommandDao().deleteBefore(timestamp) }
    }

    private fun requireFullSecureSessionAccess(operation: String) {
        if (!sessionState.hasFullSecureSessionAccess()) {
            throw SessionModeRestricted()
        }
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
