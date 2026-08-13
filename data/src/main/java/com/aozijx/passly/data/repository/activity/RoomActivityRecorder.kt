package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomActivityRecorder @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
) : ActivityRecorder {

    override suspend fun recordUsage(
        entryId: String,
        type: ActivityType
    ): AppResult<Unit> = databaseTransactions.write("activity.recordUsage") {
        val now = System.currentTimeMillis()
        entryActivityCommandDao().insertIdempotent(
            EntryActivity(entryId = entryId, activityType = type).toEntity(now)
        )
    }

    override suspend fun deleteByEntryId(entryId: String) {
        requireFullSecureSessionAccess("activity.deleteByEntryId")
        databaseSession.transaction { entryActivityCommandDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        requireFullSecureSessionAccess("activity.deleteBefore")
        databaseSession.transaction { entryActivityCommandDao().deleteBefore(timestamp) }
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
