package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.feature.autofill.core.CompleteAutofillSelection
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillResultDeliveryTest {
    @Test
    fun `temporary unlock closes only after the platform result is delivered`() = runTest {
        val store = AutofillInteractionStore()
        val lockController = RecordingLockController()
        val lease = AutofillUnlockLease(lockController)
        val interaction = store.create(
            source = AutofillInteractionSource.LEGACY,
            scope = AutofillGrantContext("com.example.app"),
            initialState = AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        val delivery = AutofillResultDelivery(
            completeSelection = CompleteAutofillSelection(
                RecordAutofillUsageUseCase(NoOpActivityRecorder),
                FinishAutofillInteraction(store, lease),
            ),
            scope = this,
        )

        assertNull(lockController.reason)

        delivery.deliverResult(
            receipt = AutofillSelectionReceipt(interaction.id, EntryId("entry-1")),
        ) {
            assertNull(lockController.reason)
        }
        runCurrent()

        assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
    }

    private class RecordingLockController : SessionLockController {
        var reason: LockReason? = null

        override suspend fun lock(reason: LockReason) {
            this.reason = reason
        }
    }

    private data object NoOpActivityRecorder : ActivityRecorder {
        override suspend fun recordUsage(entryId: String, type: ActivityType) = AppResult.Success(Unit)
        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }
}
