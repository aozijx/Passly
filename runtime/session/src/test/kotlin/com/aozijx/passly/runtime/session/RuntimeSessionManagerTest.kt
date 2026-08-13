package com.aozijx.passly.runtime.session

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class RuntimeSessionManagerTest {
    @Test
    fun `unlock owns and wipes supplied key`() = runBlocking {
        val suppliedKey = byteArrayOf(1, 2, 3, 4)
        val resource = FakeResource()
        val manager = RuntimeSessionManager(
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
    fun `soft lock resumes existing resource without requesting another key`() = runBlocking {
        val keyRequests = AtomicInteger(0)
        val resource = FakeResource()
        val manager = RuntimeSessionManager(
            resource = resource,
            keySource = SessionKeySource {
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
    fun `seal cancels long lived observers before closing resource`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val resource = FakeResource()
        val manager = RuntimeSessionManager(
            resource = resource,
            keySource = SessionKeySource { byteArrayOf(9) },
        )
        manager.unlock()

        val observer = launch {
            manager.observe {
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
        assertEquals(SecureSessionState.SEALED, manager.lockState)
        runCatching { manager.read { value } }
            .onSuccess { error("sealed session accepted a read") }
            .onFailure { assertTrue(it is SessionLockedException) }
        Unit
    }

    private class FakeResource : SessionResource<FakeHandle> {
        var openedWith: ByteArray? = null
        var closed = false

        override suspend fun open(key: ByteArray): FakeHandle {
            openedWith = key.clone()
            return FakeHandle(7)
        }

        override suspend fun close(handle: FakeHandle) {
            closed = true
        }

        override suspend fun <T> transaction(
            handle: FakeHandle,
            block: suspend FakeHandle.() -> T,
        ): T = handle.block()
    }

    private data class FakeHandle(val value: Int)
}
