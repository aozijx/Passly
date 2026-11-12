package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.*
import com.aozijx.passly.core.observability.internal.*
import com.aozijx.passly.core.observability.privacy.*
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.feature.autofill.core.CompleteAutofillSelection
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillResultDeliveryTest {
    @Test
    fun `terminal web interaction cannot deliver a previously resolved credential`() =
        runTest {
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(RecordingLockController())
            val scope = AutofillGrantContext("com.example.browser", "example.com")
            val entryId = EntryId("entry-1")
            val interaction = store.create(AutofillInteractionSource.LEGACY, scope, AutofillInteractionState.Authorized)
            store.confirmWebRecipient(interaction.id, scope, 1L)
            store.recordCandidates(interaction.id, setOf(entryId), sessionAtMs = 1L)
            store.confirmWebEntry(interaction.id, scope, entryId, 1L)
            store.beginSelection(interaction.id, entryId, 1L)
            store.finish(interaction.id, AutofillInteractionState.Cancelled)
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    secureSessionAccessState = AutofillOnlyAccessState(),
                    scope = this,
                    logger = testLogger(),
                    continuations = TraceContinuationStore(testLogger()),
                )
            var platformDeliveries = 0

            try {
                delivery.commitResult(
                    AutofillSelectionReceipt(interaction.id, entryId, webScope = scope),
                ) { platformDeliveries++ }
                throw AssertionError("expected stale web delivery to be rejected")
            } catch (expected: IllegalStateException) {
                assertEquals("Legacy web Autofill result is not confirmed for delivery", expected.message)
            }

            assertEquals(0, platformDeliveries)
        }

    @Test
    fun `trace capture failure after platform delivery still completes selection and releases lease`() =
        runTest {
            val store = AutofillInteractionStore()
            val lockController = RecordingLockController()
            val lease = AutofillUnlockLease(lockController)
            val interaction =
                store.create(
                    source = AutofillInteractionSource.LEGACY,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
            val logger = TraceFailingLogger(testLogger())
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    scope = this,
                    logger = logger,
                    continuations = TraceContinuationStore(logger),
                )
            var platformDeliveries = 0

            delivery.commitResult(
                receipt = AutofillSelectionReceipt(interaction.id, EntryId("entry-1")),
            ) { markPlatformCommitted ->
                platformDeliveries++
                markPlatformCommitted()
                logger.failTraces = true
            }
            runCurrent()

            assertEquals(1, platformDeliveries)
            assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
        }

    @Test
    fun `fatal trace capture failure after platform delivery still completes selection and releases lease`() =
        runTest {
            val store = AutofillInteractionStore()
            val lockController = RecordingLockController()
            val lease = AutofillUnlockLease(lockController)
            val interaction =
                store.create(
                    source = AutofillInteractionSource.LEGACY,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
            val logger = TraceFailingLogger(testLogger())
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    scope = this,
                    logger = logger,
                    continuations = TraceContinuationStore(logger),
                )
            val fatal = AssertionError("trace capture failed fatally")
            var platformDeliveries = 0

            val thrown =
                try {
                    delivery.commitResult(
                        receipt = AutofillSelectionReceipt(interaction.id, EntryId("entry-1")),
                    ) { markPlatformCommitted ->
                        platformDeliveries++
                        markPlatformCommitted()
                        logger.fatalCaptureFailure = fatal
                    }
                    null
                } catch (error: AssertionError) {
                    error
                }
            runCurrent()

            assertSame(fatal, thrown)
            assertEquals(1, platformDeliveries)
            assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
        }

    @Test
    fun `platform callback failure releases completion barrier and permits one successful retry`() =
        runTest {
            val store = AutofillInteractionStore()
            val lockController = RecordingLockController()
            val lease = AutofillUnlockLease(lockController)
            val interaction =
                store.create(
                    source = AutofillInteractionSource.LEGACY,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
            val logger = testLogger()
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    scope = this,
                    logger = logger,
                    continuations = TraceContinuationStore(logger),
                )
            val receipt = AutofillSelectionReceipt(interaction.id, EntryId("entry-1"))

            try {
                delivery.commitResult(receipt) { _ ->
                    throw IllegalStateException("platform rejected result")
                }
                throw AssertionError("expected platform callback failure")
            } catch (expected: IllegalStateException) {
                assertEquals("platform rejected result", expected.message)
            }

            assertEquals(AutofillInteractionState.Authorized, store.snapshot(interaction.id)?.state)
            assertNull(lockController.reason)

            var successfulDeliveries = 0
            delivery.commitResult(receipt) { markPlatformCommitted ->
                successfulDeliveries++
                markPlatformCommitted()
            }
            runCurrent()

            assertEquals(1, successfulDeliveries)
            assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
        }

    @Test
    fun `delivered result stops a later request from borrowing the completed fill session`() =
        runTest {
            val store = AutofillInteractionStore()
            val lease = AutofillUnlockLease(RecordingLockController())
            val accessState = AutofillOnlyAccessState()
            val completedInteraction =
                store.create(
                    source = AutofillInteractionSource.LEGACY,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            lease.acquireOrBorrow(completedInteraction.id, acquiredUnlock = true)
            val logger = testLogger()
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    scope = this,
                    logger = logger,
                    continuations = TraceContinuationStore(logger),
                )

            delivery.commitResult(
                receipt = AutofillSelectionReceipt(completedInteraction.id, EntryId("entry-1")),
            ) { markPlatformCommitted -> markPlatformCommitted() }
            val nextInteraction =
                async(start = CoroutineStart.UNDISPATCHED) {
                    StartAutofillInteraction(store, lease, accessState)(
                        source = AutofillInteractionSource.LEGACY,
                        scope = AutofillGrantContext("com.example.app"),
                        requireAuthentication = false,
                    )
                }

            assertFalse(nextInteraction.isCompleted)
            runCurrent()

            assertEquals(AutofillInteractionState.RequiresUnlock, nextInteraction.await().state)
        }

    @Test
    fun `successful fill expires abandoned interactions and closes temporary unlock after delivery`() =
        runTest {
            val store = AutofillInteractionStore()
            val interaction =
                store.create(
                    source = AutofillInteractionSource.LEGACY,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            val abandoned =
                store.create(
                    source = AutofillInteractionSource.CREDENTIAL_MANAGER,
                    scope = AutofillGrantContext("com.example.app"),
                    initialState = AutofillInteractionState.Authorized,
                )
            val lockController =
                RecordingLockController(
                    onLock = {
                        assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
                        assertEquals(AutofillInteractionState.Expired, store.snapshot(abandoned.id)?.state)
                    },
                )
            val lease = AutofillUnlockLease(lockController)
            lease.acquireOrBorrow(interaction.id, acquiredUnlock = true)
            lease.acquireOrBorrow(abandoned.id, acquiredUnlock = false)
            val logger = testLogger()
            val delivery =
                AutofillResultDelivery(
                    completeSelection =
                        CompleteAutofillSelection(
                            NoOpActivityRecorder,
                            FinishAutofillInteraction(store, lease),
                        ),
                    unlockLease = lease,
                    interactionStore = store,
                    scope = this,
                    logger = logger,
                    continuations = TraceContinuationStore(logger),
                )

            assertNull(lockController.reason)

            delivery.commitResult(
                receipt = AutofillSelectionReceipt(interaction.id, EntryId("entry-1")),
            ) { markPlatformCommitted ->
                assertNull(lockController.reason)
                markPlatformCommitted()
            }
            runCurrent()

            assertEquals(AutofillInteractionState.Completed, store.snapshot(interaction.id)?.state)
            assertEquals(AutofillInteractionState.Expired, store.snapshot(abandoned.id)?.state)
            assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, lockController.reason)
            assertEquals(LockTrigger.AUTOFILL_COMPLETION, lockController.trigger)
        }

    private class RecordingLockController(
        private val onLock: suspend () -> Unit = {},
    ) : SessionLockController {
        var reason: LockReason? = null
        var trigger: LockTrigger? = null

        override suspend fun lock(request: SessionLockRequest) {
            onLock()
            reason = request.reason
            trigger = request.trigger
        }
    }

    private class AutofillOnlyAccessState : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.AutofillAuthenticated(1L))

        override fun isUnlocked(): Boolean = true
    }

    private data object NoOpActivityRecorder : ActivityRecorder {
        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
            affectedFields: Set<FieldKey>,
        ) = AppResult.Success(Unit)

        override suspend fun deleteByEntryId(entryId: String) = Unit

        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private fun testLogger() =
        DefaultLogger(
            sanitizer = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.THROW),
            sinks = listOf(LogSink { }),
            lifecycleSinks = emptyList(),
            idGenerator =
                object : ObservabilityIdGenerator {
                    private var next = 0

                    private fun id(prefix: String) = "${prefix}_${++next}_value"

                    override fun eventId() = EventId(id("event"))

                    override fun traceId() = TraceId(id("trace"))

                    override fun spanId() = SpanId(id("span"))
                },
            timeSource =
                object : ObservabilityTimeSource {
                    override fun wallTimeMs() = 1_700_000_000_000L

                    override fun elapsedRealtimeMs() = 100L
                },
            processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
            secureSessionProvider = SecureSessionProvider { null },
        )

    private class TraceFailingLogger(
        private val delegate: Logger,
    ) : Logger by delegate {
        var fatalCaptureFailure: Throwable? = null
        var failTraces: Boolean = false

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T {
            if (failTraces) throw IllegalStateException("trace unavailable")
            return delegate.withRootTrace(operation, block)
        }

        override fun captureTraceToken(singleUse: Boolean): TraceToken {
            fatalCaptureFailure?.let { throw it }
            if (failTraces) throw IllegalStateException("trace unavailable")
            return delegate.captureTraceToken(singleUse)
        }
    }
}
