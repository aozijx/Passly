package com.aozijx.passly.runtime.session

import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeSessionManagerTest {
    @Test
    fun `fatal resource close error is rethrown unchanged and remains terminal`() =
        runBlocking {
            val fatal = AssertionError("resource close fatal")
            val events = mutableListOf<SessionTransitionEvent>()
            val resource = FakeResource(closeFailure = fatal)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink = SessionEventSink(events::add),
                )
            manager.unlock()

            val caught =
                try {
                    manager.seal()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertEquals(1, resource.closeAttempts)
            assertEquals(1, events.count { it.reason == SessionTransitionReason.CLOSE_FAILED })
            val retry = manager.unlock()
            assertTrue(retry is SessionUnlockResult.OpenFailed)
            assertSame(fatal, (retry as SessionUnlockResult.OpenFailed).cause)
            assertEquals(1, resource.openCount)
        }

    @Test
    fun `unlock propagates fatal error from key source`() =
        runBlocking {
            val fatal = AssertionError("key source fatal")
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { throw fatal },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `unlock propagates fatal error from resource open`() =
        runBlocking {
            val fatal = AssertionError("resource open fatal")
            val resource = FakeResource(openFailure = fatal)
            val key = byteArrayOf(9)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { key },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertEquals(1, resource.openCount)
            assertArrayEquals(byteArrayOf(0), key)
        }

    @Test
    fun `ordinary key source failure remains a key unavailable result`() =
        runBlocking {
            val failure = IOException("key unavailable")
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { throw failure },
                )

            val result = manager.unlock()

            assertTrue(result is SessionUnlockResult.KeyUnavailable)
            assertSame(failure, (result as SessionUnlockResult.KeyUnavailable).cause)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `ordinary resource open failure remains an open failed result and wipes key`() =
        runBlocking {
            val failure = IOException("resource open failed")
            val key = byteArrayOf(9)
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(openFailure = failure),
                    keySource = SessionKeySource { key },
                )

            val result = manager.unlock()

            assertTrue(result is SessionUnlockResult.OpenFailed)
            assertSame(failure, (result as SessionUnlockResult.OpenFailed).cause)
            assertArrayEquals(byteArrayOf(0), key)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `cleanup fatal error supersedes open cancellation after sealing and suppresses it`() =
        runBlocking {
            val openCancellation = CancellationException("open transition cancelled")
            val closeFatal = AssertionError("failed-open cleanup fatal")
            val resource = FakeResource(closeFailure = closeFatal)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.OPEN) throw openCancellation
                        },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(closeFatal, caught)
            assertTrue(closeFatal.suppressed.any { it === openCancellation })
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertEquals(1, resource.closeAttempts)
            assertFalse(resource.closed)
        }

    @Test
    fun `ordinary telemetry failure does not change open resume soft lock or seal outcomes`() =
        runBlocking {
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink = SessionEventSink { throw IllegalStateException("telemetry unavailable") },
                )

            assertEquals(SessionUnlockResult.Success, manager.unlock())
            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            manager.softLock()
            assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
            assertEquals(SessionUnlockResult.Success, manager.unlock())
            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            assertTrue(manager.seal().isSuccess)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertTrue(resource.closed)
        }

    @Test
    fun `ordinary close event failure preserves the resource close result`() =
        runBlocking {
            val closeFailure = IOException("database close failed")
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(closeFailure = closeFailure),
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.CLOSE_FAILED) {
                                throw IllegalStateException("telemetry unavailable")
                            }
                        },
                )
            manager.unlock()

            val result = manager.seal()

            val reportedFailure = requireNotNull(result.exceptionOrNull())
            assertEquals(closeFailure.javaClass, reportedFailure.javaClass)
            assertEquals(closeFailure.message, reportedFailure.message)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            val retry = manager.unlock()
            assertTrue(retry is SessionUnlockResult.OpenFailed)
            val retryFailure = (retry as SessionUnlockResult.OpenFailed).cause
            assertEquals(closeFailure.javaClass, retryFailure.javaClass)
            assertEquals(closeFailure.message, retryFailure.message)
        }

    @Test
    fun `ordinary drain timeout event failure does not skip lease cancellation or close`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) {
                                throw IllegalStateException("telemetry unavailable")
                            }
                        },
                )
            manager.unlock()
            val reader =
                launch {
                    manager.read {
                        leaseStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            leaseStarted.await()

            val result =
                try {
                    manager.seal(timeout = 1.milliseconds)
                } finally {
                    reader.cancelAndJoin()
                }

            assertTrue(result.isSuccess)
            assertTrue(reader.isCancelled)
            assertTrue(resource.closed)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `drain timeout cancellation propagates after cancelling leases and closing resource`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) {
                                throw CancellationException("telemetry cancelled")
                            }
                        },
                )
            manager.unlock()
            val reader =
                launch {
                    manager.read {
                        leaseStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            leaseStarted.await()

            val caught =
                try {
                    try {
                        manager.seal(timeout = 1.milliseconds)
                        null
                    } catch (error: CancellationException) {
                        error
                    }
                } finally {
                    reader.cancelAndJoin()
                }

            assertTrue(caught is CancellationException)
            assertTrue(reader.isCancelled)
            assertTrue(resource.closed)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `drain timeout fatal telemetry error propagates after cancelling leases and closing resource`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val fatal = AssertionError("telemetry fatal")
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) throw fatal
                        },
                )
            manager.unlock()
            val reader =
                launch {
                    manager.read {
                        leaseStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            leaseStarted.await()

            val caught =
                try {
                    try {
                        manager.seal(timeout = 1.milliseconds)
                        null
                    } catch (error: AssertionError) {
                        error
                    }
                } finally {
                    reader.cancelAndJoin()
                }

            assertSame(fatal, caught)
            assertTrue(reader.isCancelled)
            assertTrue(resource.closed)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `cancellation and fatal telemetry errors preserve each transition outcome`() =
        runBlocking {
            val reasons =
                listOf(
                    SessionTransitionReason.OPEN,
                    SessionTransitionReason.RESUME,
                    SessionTransitionReason.SOFT_LOCK,
                    SessionTransitionReason.SEAL,
                    SessionTransitionReason.CLOSE_FAILED,
                )
            val failures =
                listOf<Throwable>(
                    CancellationException("telemetry cancelled"),
                    AssertionError("telemetry fatal"),
                )

            reasons.forEach { reason ->
                failures.forEach { failure ->
                    val resource =
                        FakeResource(
                            closeFailure =
                                if (reason == SessionTransitionReason.CLOSE_FAILED) {
                                    IOException("database close failed")
                                } else {
                                    null
                                },
                        )
                    val manager =
                        RuntimeSessionManager(
                            resource = resource,
                            keySource = SessionKeySource { byteArrayOf(9) },
                            eventSink =
                                SessionEventSink { event ->
                                    if (event.reason == reason) throw failure
                                },
                        )
                    if (reason != SessionTransitionReason.OPEN) manager.unlock()
                    if (reason == SessionTransitionReason.RESUME) manager.softLock()

                    val caught =
                        try {
                            when (reason) {
                                SessionTransitionReason.OPEN -> manager.unlock()

                                SessionTransitionReason.RESUME -> manager.unlock()

                                SessionTransitionReason.SOFT_LOCK -> manager.softLock()

                                SessionTransitionReason.SEAL,
                                SessionTransitionReason.CLOSE_FAILED,
                                -> manager.seal()

                                else -> error("Unexpected transition $reason")
                            }
                            null
                        } catch (error: Throwable) {
                            error
                        }

                    if (failure is Error) assertSame(failure, caught) else assertTrue(caught is CancellationException)
                    when (reason) {
                        SessionTransitionReason.OPEN -> {
                            assertEquals(SecureSessionState.SEALED, manager.lockState)
                            assertTrue(resource.closed)
                            assertEquals(1, resource.closeAttempts)
                        }

                        SessionTransitionReason.RESUME -> {
                            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
                            assertFalse(resource.closed)
                        }

                        SessionTransitionReason.SOFT_LOCK -> {
                            assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
                            assertFalse(resource.closed)
                        }

                        SessionTransitionReason.SEAL -> {
                            assertEquals(SecureSessionState.SEALED, manager.lockState)
                            assertTrue(resource.closed)
                        }

                        SessionTransitionReason.CLOSE_FAILED -> {
                            assertEquals(SecureSessionState.SEALED, manager.lockState)
                            assertFalse(resource.closed)
                            assertEquals(1, resource.closeAttempts)
                            assertTrue(requireNotNull(caught).suppressed.any { it is IOException })
                        }

                        else -> {
                            error("Unexpected transition $reason")
                        }
                    }
                }
            }
        }

    @Test
    fun `fatal open transition is rethrown after closing handle and allows retry`() =
        runBlocking {
            val fatal = AssertionError("open event fatal")
            val key = byteArrayOf(9)
            val eventAttempts = AtomicInteger()
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { key },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.OPEN && eventAttempts.getAndIncrement() == 0) {
                                throw fatal
                            }
                        },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertTrue(resource.closed)
            assertEquals(1, resource.closeAttempts)
            assertArrayEquals(byteArrayOf(0), key)
            assertEquals(SessionUnlockResult.Success, manager.unlock())
            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            assertEquals(2, resource.openCount)
        }

    @Test
    fun `fatal open failure remains primary and carries cleanup failure`() =
        runBlocking {
            val fatal = AssertionError("primary open fatal")
            val cleanupFatal = AssertionError("cleanup fatal")
            val resource = FakeResource(closeFailure = cleanupFatal)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.OPEN) throw fatal
                        },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertTrue(fatal.suppressed.any { it === cleanupFatal })
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertEquals(1, resource.closeAttempts)
        }

    @Test
    fun `fatal open failure retains identity when cleanup also fails`() =
        runBlocking {
            val fatal = AssertionError("primary open fatal")
            val cleanupFailure = IOException("cleanup failed")
            val resource = FakeResource(closeFailure = cleanupFailure)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.OPEN) throw fatal
                        },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, caught)
            assertTrue(fatal.suppressed.any { it === cleanupFailure })
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            assertEquals(1, resource.closeAttempts)
        }

    @Test
    fun `unlock propagates cancellation while fetching key`() =
        runBlocking {
            val cancellation = CancellationException("key retrieval cancelled")
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { throw cancellation },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertTrue(caught is CancellationException)
            assertEquals(cancellation.message, caught?.message)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `unlock propagates cancellation while opening resource`() =
        runBlocking {
            val cancellation = CancellationException("resource open cancelled")
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(openFailure = cancellation),
                    keySource = SessionKeySource { byteArrayOf(9) },
                )

            val caught =
                try {
                    manager.unlock()
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertTrue(caught is CancellationException)
            assertEquals(cancellation.message, caught?.message)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `unlock closes resource when cancelled after resource was opened`() =
        runBlocking {
            val closeThread = AtomicReference<String?>()
            val dispatcher =
                Executors
                    .newSingleThreadExecutor { task ->
                        Thread(task, "session-resource-io")
                    }.asCoroutineDispatcher()
            val resource =
                FakeResource(
                    cancelAfterOpen = CancellationException("cancelled after open"),
                    onClose = { closeThread.set(Thread.currentThread().name) },
                )
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    ioDispatcher = dispatcher,
                )

            try {
                val caught =
                    try {
                        manager.unlock()
                        null
                    } catch (error: CancellationException) {
                        error
                    }

                assertTrue(caught is CancellationException)
                assertEquals("cancelled after open", caught?.message)
                assertTrue(resource.closed)
                assertTrue(closeThread.get()?.startsWith("session-resource-io") == true)
                assertEquals(SecureSessionState.SEALED, manager.lockState)
            } finally {
                dispatcher.close()
            }
        }

    @Test
    fun `seal propagates close cancellation and permanently rejects same-process reopen`() =
        runBlocking {
            val cancellation = CancellationException("resource close cancelled")
            val resource = FakeResource(closeFailure = cancellation)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                )
            manager.unlock()

            val caught =
                try {
                    manager.seal()
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertTrue(caught is CancellationException)
            assertEquals(cancellation.message, caught?.message)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            val reopen = manager.unlock()
            assertTrue(reopen is SessionUnlockResult.OpenFailed)
            assertTrue((reopen as SessionUnlockResult.OpenFailed).cause is CancellationException)
            assertEquals(1, resource.openCount)
            assertEquals(1, resource.closeAttempts)
            assertTrue(!resource.closed)
        }

    @Test
    fun `seal closes resource on configured io dispatcher`() =
        runBlocking {
            val closeThread = AtomicReference<String?>()
            val dispatcher =
                Executors
                    .newSingleThreadExecutor { task ->
                        Thread(task, "session-resource-io")
                    }.asCoroutineDispatcher()
            val resource = FakeResource(onClose = { closeThread.set(Thread.currentThread().name) })
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    ioDispatcher = dispatcher,
                )

            try {
                manager.unlock()
                manager.seal()

                assertTrue(closeThread.get()?.startsWith("session-resource-io") == true)
            } finally {
                dispatcher.close()
            }
        }

    @Test
    fun `unlock owns and wipes supplied key`() =
        runBlocking {
            val suppliedKey = byteArrayOf(1, 2, 3, 4)
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { suppliedKey },
                )

            assertEquals(SessionUnlockResult.Success, manager.unlock())

            assertArrayEquals(byteArrayOf(1, 2, 3, 4), resource.openedWith)
            assertArrayEquals(byteArrayOf(0, 0, 0, 0), suppliedKey)
            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            assertEquals(7, manager.read { value })
        }

    @Test
    fun `soft lock resumes existing resource without requesting another key`() =
        runBlocking {
            val keyRequests = AtomicInteger(0)
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource =
                        SessionKeySource {
                            keyRequests.incrementAndGet()
                            byteArrayOf(9)
                        },
                )

            manager.unlock()
            manager.softLock()
            assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)

            assertEquals(SessionUnlockResult.Success, manager.unlock())
            assertEquals(1, keyRequests.get())
            assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
        }

    @Test
    fun `lock generation advances for each state transition`() =
        runBlocking {
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { byteArrayOf(9) },
                )

            assertEquals(0L, manager.lockGenerationFlow.value)
            manager.unlock()
            assertEquals(1L, manager.lockGenerationFlow.value)
            manager.softLock()
            assertEquals(2L, manager.lockGenerationFlow.value)
            manager.unlock()
            assertEquals(3L, manager.lockGenerationFlow.value)
            manager.seal()
            assertEquals(4L, manager.lockGenerationFlow.value)
        }

    @Test
    fun `seal cancels long lived observers before closing resource`() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val events = mutableListOf<SessionTransitionEvent>()
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink = SessionEventSink(events::add),
                )
            manager.unlock()

            val observer =
                launch {
                    manager
                        .observe {
                            flow<Int> {
                                started.complete(Unit)
                                awaitCancellation()
                            }
                        }.collect()
                }
            started.await()

            manager.seal(timeout = 1.milliseconds)
            observer.join()

            assertTrue(observer.isCancelled)
            assertTrue(resource.closed)
            val timeout = events.single { it.reason == SessionTransitionReason.DRAIN_TIMEOUT }
            assertEquals(SecureSessionState.UNLOCKED, timeout.fromState)
            assertEquals(SecureSessionState.SEALED, timeout.toState)
            assertEquals(SessionTransitionOwner.SESSION_RUNTIME, timeout.owner)
            assertEquals(SessionLockType.SEALED, timeout.lockType)
            assertEquals(1L, timeout.timeoutMs)
            assertEquals(1, timeout.activeLeaseCount)
            assertEquals(null, timeout.error)
            assertTrue(timeout.elapsedMs >= 0)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            runCatching { manager.read { value } }
                .onSuccess { error("sealed session accepted a read") }
                .onFailure { assertTrue(it is SessionLockedException) }
            Unit
        }

    @Test
    fun `seal waits for active lease to drain before closing resource`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val releaseLease = CompletableDeferred<Unit>()
            val resource = FakeResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                )
            manager.unlock()

            val reader =
                async {
                    manager.read {
                        leaseStarted.complete(Unit)
                        releaseLease.await()
                        value
                    }
                }
            leaseStarted.await()
            val sealing = async { manager.seal(timeout = 500.milliseconds) }

            assertTrue(!resource.closed)
            releaseLease.complete(Unit)
            assertEquals(7, reader.await())
            sealing.await()

            assertTrue(resource.closed)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
        }

    @Test
    fun `seal timeout cancels active write before closing resource`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val leaseFinished = CompletableDeferred<Unit>()
            val leaseFinishedBeforeClose = AtomicReference(false)
            val resource = FakeResource(onClose = { leaseFinishedBeforeClose.set(leaseFinished.isCompleted) })
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                )
            manager.unlock()

            val writer =
                async {
                    manager.write {
                        leaseStarted.complete(Unit)
                        try {
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            leaseFinished.complete(Unit)
                        }
                    }
                }
            leaseStarted.await()

            try {
                manager.seal(timeout = 1.milliseconds)

                assertTrue("timed-out write must be cancelled", writer.isCancelled)
                assertTrue("write must finish cleanup before resource close", leaseFinished.isCompleted)
                assertTrue(leaseFinishedBeforeClose.get())
                assertTrue(resource.closed)
                assertEquals(SecureSessionState.SEALED, manager.lockState)
            } finally {
                writer.cancelAndJoin()
            }
        }

    @Test
    fun `close failure remains terminal until process restart`() =
        runBlocking {
            val failure = IllegalStateException("close failed")
            val events = mutableListOf<SessionTransitionEvent>()
            val resource = FakeResource(closeFailure = failure)
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink = SessionEventSink(events::add),
                )
            manager.unlock()

            val result = manager.seal()

            assertTrue(result.isFailure)
            val reportedFailure = requireNotNull(result.exceptionOrNull())
            assertEquals(failure.javaClass, reportedFailure.javaClass)
            assertEquals(failure.message, reportedFailure.message)
            val closeFailure = events.single { it.reason == SessionTransitionReason.CLOSE_FAILED }
            assertEquals(SecureSessionState.SEALED, closeFailure.fromState)
            assertEquals(SecureSessionState.SEALED, closeFailure.toState)
            assertEquals(SessionLockType.SEALED, closeFailure.lockType)
            assertEquals(failure.javaClass, closeFailure.error?.javaClass)
            assertEquals(failure.message, closeFailure.error?.message)
            assertEquals(SecureSessionState.SEALED, manager.lockState)
            val reopen = manager.unlock()
            assertTrue(reopen is SessionUnlockResult.OpenFailed)
            assertSame(reportedFailure, (reopen as SessionUnlockResult.OpenFailed).cause)
            assertEquals("same-process unlock must not open another handle", 1, resource.openCount)
            assertEquals("same-process unlock must not retry close", 1, resource.closeAttempts)

            val afterProcessRestart =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { byteArrayOf(9) },
                )
            assertEquals(SessionUnlockResult.Success, afterProcessRestart.unlock())
        }

    @Test
    fun `open resume and soft lock emit complete typed transitions`() =
        runBlocking {
            val events = mutableListOf<SessionTransitionEvent>()
            val manager =
                RuntimeSessionManager(
                    resource = FakeResource(),
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink = SessionEventSink(events::add),
                )

            manager.unlock()
            manager.softLock()
            manager.unlock()

            assertEquals(
                listOf(
                    Triple(SecureSessionState.SEALED, SecureSessionState.UNLOCKED, SessionTransitionReason.OPEN),
                    Triple(SecureSessionState.UNLOCKED, SecureSessionState.SOFT_LOCKED, SessionTransitionReason.SOFT_LOCK),
                    Triple(SecureSessionState.SOFT_LOCKED, SecureSessionState.UNLOCKED, SessionTransitionReason.RESUME),
                ),
                events.map { Triple(it.fromState, it.toState, it.reason) },
            )
            assertTrue(events.all { it.owner == SessionTransitionOwner.SESSION_RUNTIME })
            assertEquals(
                listOf(SessionLockType.NONE, SessionLockType.SOFT, SessionLockType.NONE),
                events.map(SessionTransitionEvent::lockType),
            )
            assertTrue(events.all { it.elapsedMs >= 0 && it.timeoutMs == null && it.error == null })
        }

    private class FakeResource(
        private val openFailure: Throwable? = null,
        private val closeFailure: Throwable? = null,
        private val cancelAfterOpen: CancellationException? = null,
        private val onClose: () -> Unit = {},
    ) : SessionResource<FakeHandle> {
        var openedWith: ByteArray? = null
        var closed = false
        var openCount = 0
        var closeAttempts = 0

        override suspend fun open(key: ByteArray): FakeHandle {
            openCount += 1
            openedWith = key.clone()
            openFailure?.let { throw it }
            cancelAfterOpen?.let { currentCoroutineContext().cancel(it) }
            return FakeHandle(7)
        }

        override suspend fun close(handle: FakeHandle) {
            closeAttempts += 1
            onClose()
            closeFailure?.let { throw it }
            closed = true
        }

        override suspend fun <T> transaction(
            handle: FakeHandle,
            block: suspend FakeHandle.() -> T,
        ): T = handle.block()
    }

    private data class FakeHandle(
        val value: Int,
    )
}
