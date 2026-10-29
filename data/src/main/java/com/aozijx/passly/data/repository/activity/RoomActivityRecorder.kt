package com.aozijx.passly.data.repository.activity

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomActivityRecorder @Inject constructor(
    private val databaseTransactions: DatabaseTransactionRunner,
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock,
) : ActivityRecorder {

    override suspend fun recordUsage(
        entryId: String,
        type: ActivityType
    ): AppResult<Unit> {
        require(type.isUsage) { "Usage recorder only accepts usage events" }
        val persist: suspend AppDatabase.() -> Unit = {
            activityWriter.recordActivity(this, entryId, type, clock.now(), EntryActivityContext.Usage)
        }
        return if (type == ActivityType.AUTOFILL) {
            databaseTransactions.writeForAutofill("activity_record_usage", persist)
        } else {
            databaseTransactions.write("activity_record_usage", persist)
        }
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

}
