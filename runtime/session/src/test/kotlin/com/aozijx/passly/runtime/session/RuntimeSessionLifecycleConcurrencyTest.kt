package com.aozijx.passly.runtime.session

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeSessionLifecycleConcurrencyTest {
    @Test
    fun `concurrent first unlock requests open resource once`() =
        runBlocking {
            val openStarted = CompletableDeferred<Unit>()
            val releaseOpen = CompletableDeferred<Unit>()
            val resource =
                TestResource(
                    onOpen = {
                        openStarted.complete(Unit)
                        releaseOpen.await()
                    },
                )
            val keyRequests =
                java.util.concurrent.atomic
                    .AtomicInteger()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource =
                        SessionKeySource {
                            keyRequests.incrementAndGet()
                            byteArrayOf(9)
                        },
                )

            val firstUnlock = async { manager.unlock() }
            openStarted.await()
            val secondUnlock = async { manager.unlock() }

            try {
                delay(25)
                assertEquals("only the serialized owner should request a key", 1, keyRequests.get())
                releaseOpen.complete(Unit)

                assertEquals(SessionUnlockResult.Success, firstUnlock.await())
                assertEquals(SessionUnlockResult.Success, secondUnlock.await())
                assertEquals(1, resource.openCount)
                assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            } finally {
                releaseOpen.complete(Unit)
                firstUnlock.cancelAndJoin()
                secondUnlock.cancelAndJoin()
            }
        }

    @Test
    fun `seal waits for an in flight open and closes its handle`() =
        runBlocking {
            val openStarted = CompletableDeferred<Unit>()
            val releaseOpen = CompletableDeferred<Unit>()
            val sealEmitted = CompletableDeferred<Unit>()
            val resource =
                TestResource(
                    onOpen = {
                        openStarted.complete(Unit)
                        releaseOpen.await()
                    },
                )
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.SEAL) sealEmitted.complete(Unit)
                        },
                )
            val opening = async { manager.unlock() }
            openStarted.await()
            val sealing = async { manager.seal(timeout = 1.milliseconds) }

            try {
                delay(25)
                assertFalse("seal must not finish before the pending open", sealing.isCompleted)
                assertFalse("seal must not close a handle that open has not published", sealEmitted.isCompleted)
                releaseOpen.complete(Unit)

                assertEquals(SessionUnlockResult.Success, opening.await())
                assertTrue(sealing.await().isSuccess)
                assertEquals(1, resource.openCount)
                assertEquals(1, resource.closeCount)
                assertTrue(resource.closed)
                assertEquals(SecureSessionState.SEALED, manager.lockState)
            } finally {
                releaseOpen.complete(Unit)
                opening.cancelAndJoin()
                sealing.cancelAndJoin()
            }
        }

    @Test
    fun `unlock waits for seal drain then opens a fresh handle`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val releaseLease = CompletableDeferred<Unit>()
            val drainTimedOut = CompletableDeferred<Unit>()
            val resource = TestResource()
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) drainTimedOut.complete(Unit)
                        },
                )
            manager.unlock()
            val reader =
                async {
                    manager.read {
                        leaseStarted.complete(Unit)
                        withContext(NonCancellable) { releaseLease.await() }
                        value
                    }
                }
            leaseStarted.await()
            val sealing = async { manager.seal(timeout = 1.milliseconds) }

            try {
                drainTimedOut.await()
                val reopening = async { manager.unlock() }
                delay(25)

                assertEquals(SecureSessionState.SEALED, manager.lockState)
                assertFalse("unlock must wait until seal closes the old handle", reopening.isCompleted)
                releaseLease.complete(Unit)
                reader.join()
                assertTrue(sealing.await().isSuccess)

                assertEquals(SessionUnlockResult.Success, reopening.await())
                assertEquals(2, resource.openCount)
                assertEquals(1, resource.closeCount)
                assertFalse(resource.closed)
                assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            } finally {
                releaseLease.complete(Unit)
                reader.cancelAndJoin()
                sealing.cancelAndJoin()
            }
        }

    @Test
    fun `unlock waits for soft lock event before resuming the session`() =
        runBlocking {
            val softLockEmitted = CompletableDeferred<Unit>()
            val releaseSoftLock = CompletableDeferred<Unit>()
            val manager =
                RuntimeSessionManager(
                    resource = TestResource(),
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.SOFT_LOCK) {
                                softLockEmitted.complete(Unit)
                                releaseSoftLock.await()
                            }
                        },
                )
            manager.unlock()
            val softLocking = async { manager.softLock() }
            softLockEmitted.await()
            val resuming = async { manager.unlock() }

            try {
                delay(25)
                assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
                assertFalse("resume must wait for the soft-lock transition owner", resuming.isCompleted)
                releaseSoftLock.complete(Unit)
                softLocking.await()
                assertEquals(SessionUnlockResult.Success, resuming.await())
                assertEquals(SecureSessionState.UNLOCKED, manager.lockState)
            } finally {
                releaseSoftLock.complete(Unit)
                softLocking.cancelAndJoin()
                resuming.cancelAndJoin()
            }
        }

    @Test
    fun `soft lock rejects new leases and drains active leases without closing the handle`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val releaseLease = CompletableDeferred<Unit>()
            val drainTimedOut = CompletableDeferred<Unit>()
            val leaseFinished =
                java.util.concurrent.atomic
                    .AtomicBoolean(false)
            val closedBeforeLeaseFinished =
                java.util.concurrent.atomic
                    .AtomicBoolean(false)
            val resource =
                TestResource(
                    onClose = { closedBeforeLeaseFinished.set(!leaseFinished.get()) },
                )
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) drainTimedOut.complete(Unit)
                        },
                )
            manager.unlock()
            val reader =
                async {
                    manager.read {
                        try {
                            leaseStarted.complete(Unit)
                            withContext(NonCancellable) { releaseLease.await() }
                            value
                        } finally {
                            leaseFinished.set(true)
                        }
                    }
                }
            leaseStarted.await()
            val softLocking = async { manager.softLock(timeout = 1.milliseconds) }

            try {
                drainTimedOut.await()
                assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
                assertFalse("soft lock must await the active lease", softLocking.isCompleted)
                assertFalse("soft lock must keep the open resource", resource.closed)
                assertEquals(0, resource.closeCount)

                val rejectedRead =
                    try {
                        manager.read { value }
                        null
                    } catch (failure: SessionLockedException) {
                        failure
                    }
                assertTrue(rejectedRead is SessionLockedException)

                releaseLease.complete(Unit)
                reader.join()
                softLocking.await()

                assertTrue(reader.isCancelled)
                assertTrue(leaseFinished.get())
                assertFalse(closedBeforeLeaseFinished.get())
                assertFalse(resource.closed)
                assertEquals(0, resource.closeCount)
                assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
            } finally {
                releaseLease.complete(Unit)
                reader.cancelAndJoin()
                softLocking.cancelAndJoin()
            }
        }

    @Test
    fun `soft lock cancellation drains active leases before propagating`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val releaseLease = CompletableDeferred<Unit>()
            val resource = TestResource()
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
                        withContext(NonCancellable) { releaseLease.await() }
                        value
                    }
                }
            leaseStarted.await()
            val softLocking = async { manager.softLock() }
            while (manager.lockState != SecureSessionState.SOFT_LOCKED) delay(1)

            try {
                softLocking.cancel()
                yield()

                assertFalse("cancelled soft lock must keep the active handle open", resource.closed)
                assertFalse("soft lock cleanup must wait for the active lease", softLocking.isCompleted)

                releaseLease.complete(Unit)
                reader.join()
                softLocking.join()

                assertTrue(reader.isCancelled)
                assertTrue(softLocking.isCancelled)
                assertFalse(resource.closed)
                assertEquals(0, resource.closeCount)
                assertEquals(SecureSessionState.SOFT_LOCKED, manager.lockState)
            } finally {
                releaseLease.complete(Unit)
                reader.cancelAndJoin()
                softLocking.cancelAndJoin()
            }
        }

    @Test
    fun `seal cancellation still drains and closes before propagating`() =
        runBlocking {
            val leaseStarted = CompletableDeferred<Unit>()
            val releaseLease = CompletableDeferred<Unit>()
            val drainTimedOut = CompletableDeferred<Unit>()
            val transactionFinished =
                java.util.concurrent.atomic
                    .AtomicBoolean(false)
            val closedBeforeTransactionFinished =
                java.util.concurrent.atomic
                    .AtomicBoolean(false)
            val resource =
                TestResource(
                    onClose = { closedBeforeTransactionFinished.set(!transactionFinished.get()) },
                    onTransactionFinished = { transactionFinished.set(true) },
                )
            val manager =
                RuntimeSessionManager(
                    resource = resource,
                    keySource = SessionKeySource { byteArrayOf(9) },
                    eventSink =
                        SessionEventSink { event ->
                            if (event.reason == SessionTransitionReason.DRAIN_TIMEOUT) drainTimedOut.complete(Unit)
                        },
                )
            manager.unlock()
            val writer =
                async {
                    manager.write {
                        leaseStarted.complete(Unit)
                        withContext(NonCancellable) { releaseLease.await() }
                        value
                    }
                }
            leaseStarted.await()
            val sealing = async { manager.seal(timeout = 5.seconds) }
            while (manager.lockState != SecureSessionState.SEALED) delay(1)

            try {
                sealing.cancel()
                delay(150)

                assertTrue("cancellation must transfer cleanup to seal", drainTimedOut.isCompleted)
                assertFalse("the active transaction must keep its handle open", resource.closed)
                assertFalse("seal must wait for the cancelled lease to exit", sealing.isCompleted)

                releaseLease.complete(Unit)
                writer.join()
                sealing.join()

                assertTrue(sealing.isCancelled)
                assertTrue(transactionFinished.get())
                assertFalse(closedBeforeTransactionFinished.get())
                assertTrue(resource.closed)
                assertEquals(1, resource.closeCount)
                assertEquals(SecureSessionState.SEALED, manager.lockState)
            } finally {
                releaseLease.complete(Unit)
                writer.cancelAndJoin()
                sealing.cancelAndJoin()
            }
        }

    private class TestResource(
        private val onOpen: suspend () -> Unit = {},
        private val onClose: () -> Unit = {},
        private val onTransactionFinished: () -> Unit = {},
    ) : SessionResource<TestHandle> {
        var openCount = 0
        var closeCount = 0
        var closed = false

        override suspend fun open(key: ByteArray): TestHandle {
            openCount += 1
            onOpen()
            closed = false
            return TestHandle(openCount)
        }

        override suspend fun close(handle: TestHandle) {
            closeCount += 1
            onClose()
            closed = true
        }

        override suspend fun <T> transaction(
            handle: TestHandle,
            block: suspend TestHandle.() -> T,
        ): T =
            try {
                handle.block()
            } finally {
                onTransactionFinished()
            }
    }

    private data class TestHandle(
        val value: Int,
    )
}
