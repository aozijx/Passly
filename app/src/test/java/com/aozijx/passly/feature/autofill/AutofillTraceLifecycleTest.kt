package com.aozijx.passly.feature.autofill

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
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
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillResultDelivery
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillTraceLifecycleTest {
    @Test
    fun `delivery and session close remain ordered in one trace with separate spans`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val continuations = TraceContinuationStore(logger)
        val store = AutofillInteractionStore()
        val lockController = RecordingLockController()
        val lease = AutofillUnlockLease(lockController)
        val interaction = store.create(
            AutofillInteractionSource.LEGACY,
            AutofillGrantContext("com.example.app"),
            AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            continuations.capture(interaction.id.value)
        }
        val delivery = AutofillResultDelivery(
            completeSelection = CompleteAutofillSelection(
                RecordAutofillUsageUseCase(NoOpActivityRecorder),
                FinishAutofillInteraction(store, lease),
            ),
            unlockLease = lease,
            scope = this,
            logger = logger,
            continuations = continuations,
        )

        delivery.deliverResult(AutofillSelectionReceipt(interaction.id, EntryId("entry-1"))) {
            assertNull(lockController.reason)
        }
        runCurrent()

        assertEquals(
            listOf(
                Events.Autofill.ResultDeliveryStarted,
                Events.Autofill.ResultDelivered,
                Events.Autofill.SessionCloseStarted,
                Events.Autofill.SessionClosed,
            ),
            records.map { it.event },
        )
        assertEquals(1, records.map { it.trace.traceId }.distinct().size)
        assertNotEquals(records[0].trace.spanId, records[2].trace.spanId)
        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
    }

    @Test
    fun `platform delivery failure is logged and does not close the session`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val continuations = TraceContinuationStore(logger)
        val store = AutofillInteractionStore()
        val lockController = RecordingLockController()
        val lease = AutofillUnlockLease(lockController)
        val interaction = store.create(
            AutofillInteractionSource.CREDENTIAL_MANAGER,
            AutofillGrantContext("com.example.app"),
            AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        logger.withRootTrace(Operations.AutofillCredentialRequest) {
            continuations.capture(interaction.id.value)
        }
        val delivery = AutofillResultDelivery(
            CompleteAutofillSelection(
                RecordAutofillUsageUseCase(NoOpActivityRecorder),
                FinishAutofillInteraction(store, lease),
            ),
            lease,
            this,
            logger,
            continuations,
        )

        assertThrows(IllegalStateException::class.java) {
            delivery.deliverResult(AutofillSelectionReceipt(interaction.id, EntryId("entry-1"))) {
                error("raw package com.secret.target must not escape")
            }
        }
        runCurrent()

        assertEquals(
            listOf(Events.Autofill.ResultDeliveryStarted, Events.Autofill.ResultDeliveryFailed),
            records.map { it.event },
        )
        assertNull(lockController.reason)
        assertEquals(AutofillInteractionState.Authorized, store.snapshot(interaction.id)?.state)
        assertEquals("IllegalStateException", records.last().error?.exception?.type?.value)
    }

    @Test
    fun `missing pending intent continuation creates a new root and records resume unavailable`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val continuations = TraceContinuationStore(logger)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(RecordingLockController())
        val interaction = store.create(
            AutofillInteractionSource.LEGACY,
            AutofillGrantContext("com.example.app"),
            AutofillInteractionState.Authorized,
        )
        lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
        val delivery = AutofillResultDelivery(
            CompleteAutofillSelection(
                RecordAutofillUsageUseCase(NoOpActivityRecorder),
                FinishAutofillInteraction(store, lease),
            ),
            lease,
            this,
            logger,
            continuations,
        )

        delivery.deliverResult(AutofillSelectionReceipt(interaction.id, EntryId("entry-1"))) {}
        runCurrent()

        assertEquals(Events.Application.TraceResumeUnavailable, records.first().event)
        assertEquals(1, records.map { it.trace.traceId }.distinct().size)
        assertEquals(Events.Autofill.SessionClosed, records.last().event)
    }

    @Test
    fun `legacy and credential result callbacks remain independent`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val continuations = TraceContinuationStore(logger)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(RecordingLockController())
        val legacy = store.create(
            AutofillInteractionSource.LEGACY,
            AutofillGrantContext("com.legacy.app"),
            AutofillInteractionState.Authorized,
        )
        val credential = store.create(
            AutofillInteractionSource.CREDENTIAL_MANAGER,
            AutofillGrantContext("com.credential.app"),
            AutofillInteractionState.Authorized,
        )
        val delivery = AutofillResultDelivery(
            CompleteAutofillSelection(
                RecordAutofillUsageUseCase(NoOpActivityRecorder),
                FinishAutofillInteraction(store, lease),
            ),
            lease,
            this,
            logger,
            continuations,
        )
        val callbacks = mutableListOf<String>()

        delivery.deliverResult(AutofillSelectionReceipt(legacy.id, EntryId("legacy-entry"))) {
            callbacks += "legacy_dataset"
        }
        delivery.deliverResult(AutofillSelectionReceipt(credential.id, EntryId("credential-entry"))) {
            callbacks += "credential_intent"
        }
        runCurrent()

        assertEquals(listOf("legacy_dataset", "credential_intent"), callbacks)
        assertEquals(AutofillInteractionState.Completed, store.snapshot(legacy.id)?.state)
        assertEquals(AutofillInteractionState.Completed, store.snapshot(credential.id)?.state)
        assertEquals(2, records.count { it.event == Events.Autofill.ResultDelivered })
    }

    private fun logger(records: MutableList<SanitizedLogRecord>) = DefaultLogger(
        sanitizer = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.THROW),
        sinks = listOf(LogSink(records::add)),
        lifecycleSinks = emptyList(),
        idGenerator = SequentialIdGenerator(),
        timeSource = object : ObservabilityTimeSource {
            override fun wallTimeMs() = 1_700_000_000_000L
            override fun elapsedRealtimeMs() = 100L
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { SecureSessionId("secure_session") },
    )

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0
        private fun id(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(id("event"))
        override fun traceId() = TraceId(id("trace"))
        override fun spanId() = SpanId(id("span"))
        override fun diagnosticId() =
            DiagnosticId.fromEntropy(ByteArray(8) { index -> (index + ++next).toByte() })
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
