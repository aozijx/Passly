package com.aozijx.passly.core.platform.clipboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardClearSchedulerTest {

    @Test
    fun `scheduled token is cleared only after delay`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.replace("first", 30_000, publish = {}) { cleared += it }
        advanceTimeBy(29_999)
        assertEquals(emptyList<String>(), cleared)

        advanceUntilIdle()
        assertEquals(listOf("first"), cleared)
    }

    @Test
    fun `new replacement replaces previous token`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.replace("first", 30_000, publish = {}) { cleared += it }
        scheduler.replace("second", 15_000, publish = {}) { cleared += it }
        advanceUntilIdle()

        assertEquals(listOf("second"), cleared)
    }

    @Test
    fun `cancel prevents pending clear`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.replace("first", 15_000, publish = {}) { cleared += it }
        scheduler.cancel()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), cleared)
    }

    @Test
    fun `interleaved copies do not cancel the active clip expiration`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scheduler = ClipboardClearScheduler(scope, Dispatchers.Default)
        val activeToken = AtomicReference<String?>(null)
        val cleared = CountDownLatch(1)
        val firstPublished = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondCopyStarted = CountDownLatch(1)

        val first = Thread {
            scheduler.replace(
                ownershipToken = "first",
                delayMillis = 10_000,
                publish = {
                    activeToken.set("first")
                    firstPublished.countDown()
                    releaseFirst.await()
                },
                clear = { token -> if (activeToken.compareAndSet(token, null)) cleared.countDown() },
            )
        }
        val second = Thread {
            firstPublished.await()
            secondCopyStarted.countDown()
            scheduler.replace(
                ownershipToken = "second",
                delayMillis = 20,
                publish = { activeToken.set("second") },
                clear = { token -> if (activeToken.compareAndSet(token, null)) cleared.countDown() },
            )
        }

        try {
            first.start()
            second.start()
            assertTrue("second copy should start", secondCopyStarted.await(2, TimeUnit.SECONDS))
            releaseFirst.countDown()
            first.join(2_000)
            second.join(2_000)

            assertFalse("copy workers should finish", first.isAlive || second.isAlive)
            assertTrue("the currently published clip should be cleared by its timer", cleared.await(2, TimeUnit.SECONDS))
            assertNull(activeToken.get())
        } finally {
            releaseFirst.countDown()
            scheduler.cancel()
            scope.cancel()
        }
    }

    @Test
    fun `older timer cannot clear a newer clip during its ownership check`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scheduler = ClipboardClearScheduler(scope, Dispatchers.Default)
        val activeToken = AtomicReference<String?>("first")
        val oldTimerCheckedOwnership = CountDownLatch(1)
        val releaseOldTimer = CountDownLatch(1)
        val newCopyStarted = CountDownLatch(1)
        val newClipPublished = CountDownLatch(1)

        scheduler.replace(
            ownershipToken = "first",
            delayMillis = 20,
            publish = { activeToken.set("first") },
            clear = { token ->
                if (activeToken.get() == token) {
                    oldTimerCheckedOwnership.countDown()
                    releaseOldTimer.await()
                    activeToken.set(null)
                }
            },
        )
        val second = Thread {
            oldTimerCheckedOwnership.await()
            newCopyStarted.countDown()
            scheduler.replace(
                ownershipToken = "second",
                delayMillis = 10_000,
                publish = {
                    activeToken.set("second")
                    newClipPublished.countDown()
                },
                clear = { token -> activeToken.compareAndSet(token, null) },
            )
        }

        try {
            second.start()
            assertTrue("older timer should reach its ownership check", oldTimerCheckedOwnership.await(2, TimeUnit.SECONDS))
            assertTrue("new copy should start", newCopyStarted.await(2, TimeUnit.SECONDS))
            assertFalse("new clip must wait for old clear to finish", newClipPublished.await(100, TimeUnit.MILLISECONDS))
            releaseOldTimer.countDown()
            assertTrue("new clip should publish after old clear finishes", newClipPublished.await(2, TimeUnit.SECONDS))
            second.join(2_000)

            assertFalse("copy worker should finish", second.isAlive)
            assertEquals("second", activeToken.get())
        } finally {
            releaseOldTimer.countDown()
            scheduler.cancel()
            scope.cancel()
        }
    }
}
