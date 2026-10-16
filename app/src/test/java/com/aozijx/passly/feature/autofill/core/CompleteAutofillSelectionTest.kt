package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteAutofillSelectionTest {
    @Test
    fun `successful fill records usage and closes its temporary unlock`() = runBlocking {
        val store = AutofillInteractionStore()
        val lockController = RecordingLockController()
        val lease = AutofillUnlockLease(lockController)
        val interaction = store.create(
            source = AutofillInteractionSource.LEGACY,
            scope = AutofillGrantContext("com.example.app"),
            initialState = AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        val activityRecorder = RecordingActivityRecorder {
            assertEquals(null, lockController.reason)
        }
        val operation = CompleteAutofillSelection(
            recordUsage = RecordAutofillUsageUseCase(activityRecorder),
            finishInteraction = FinishAutofillInteraction(store, lease),
        )

        operation(interaction.id, EntryId("entry-1"))

        assertEquals("entry-1", activityRecorder.recordedEntryId)
        assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
    }

    @Test
    fun `duplicate delivery receipt records usage only once`() = runBlocking {
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(RecordingLockController())
        val interaction = store.create(
            source = AutofillInteractionSource.CREDENTIAL_MANAGER,
            scope = AutofillGrantContext("com.example.app"),
            initialState = AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        val activityRecorder = RecordingActivityRecorder()
        val operation = CompleteAutofillSelection(
            recordUsage = RecordAutofillUsageUseCase(activityRecorder),
            finishInteraction = FinishAutofillInteraction(store, lease),
        )

        operation(interaction.id, EntryId("entry-1"))
        operation(interaction.id, EntryId("entry-1"))

        assertEquals(1, activityRecorder.recordCount)
        assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
    }

    private class RecordingLockController : SessionLockController {
        var reason: LockReason? = null

        override suspend fun lock(request: SessionLockRequest) {
            reason = request.reason
        }
    }

    private class RecordingActivityRecorder(
        private val onRecord: () -> Unit = {},
    ) : ActivityRecorder {
        var recordedEntryId: String? = null
        var recordCount = 0

        override suspend fun recordUsage(entryId: String, type: ActivityType): AppResult<Unit> {
            onRecord()
            recordedEntryId = entryId
            recordCount++
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }
}
