package com.aozijx.passly.runtime.session

import com.aozijx.passly.domain.authentication.SecureSessionState
import com.aozijx.passly.domain.authentication.SessionLockedException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class SessionLeaseGate<Handle : Any>(
    private val resource: SessionResource<Handle>,
    private val eventSink: SessionEventSink,
) {
    private companion object {
        const val DRAIN_POLL_MS = 50L
        const val POST_CANCEL_DELAY_MS = 100L
    }

    private val mutableLockState = MutableStateFlow(SecureSessionState.SEALED)
    val lockState: StateFlow<SecureSessionState> = mutableLockState.asStateFlow()

    private val activeLeases = AtomicInteger(0)
    private val observerJobs = ConcurrentHashMap.newKeySet<Job>()
    private val leaseMutex = Mutex()
    private val resourceMutex = Mutex()

    private var handle: Handle? = null

    suspend fun <T> read(block: suspend Handle.() -> T): T {
        acquireLease()
        return try {
            resolveHandle().block()
        } finally {
            releaseLease()
        }
    }

    suspend fun <T> write(block: suspend Handle.() -> T): T {
        acquireLease()
        return try {
            val current = resolveHandle()
            resource.transaction(current, block)
        } finally {
            releaseLease()
        }
    }

    fun <T> observe(block: suspend Handle.() -> Flow<T>): Flow<T> = flow {
        val collectorJob = currentCoroutineContext()[Job]
        acquireLease()
        if (collectorJob != null) observerJobs += collectorJob
        try {
            resolveHandle().block().collect { value ->
                ensureUnlocked()
                emit(value)
            }
        } finally {
            if (collectorJob != null) observerJobs -= collectorJob
            releaseLease()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resumeIfOpen(): Boolean = resourceMutex.withLock {
        if (handle == null) return false
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.UNLOCKED }
        eventSink.emit(SessionRuntimeEvent.RESUMED, null)
        true
    }

    suspend fun open(key: ByteArray): SessionUnlockResult = try {
        val opened = resource.open(key)
        resourceMutex.withLock { handle = opened }
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.UNLOCKED }
        eventSink.emit(SessionRuntimeEvent.OPENED, null)
        SessionUnlockResult.Success
    } catch (error: Throwable) {
        resourceMutex.withLock { handle = null }
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.SEALED }
        eventSink.emit(SessionRuntimeEvent.OPEN_FAILED, error)
        SessionUnlockResult.OpenFailed(error)
    }

    suspend fun softLock() {
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.SOFT_LOCKED }
        eventSink.emit(SessionRuntimeEvent.SOFT_LOCKED, null)
    }

    suspend fun seal(timeout: Duration) {
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.SEALED }

        val drained = withTimeoutOrNull(timeout) {
            while (activeLeases.get() > 0) delay(DRAIN_POLL_MS)
        }
        if (drained == null) {
            eventSink.emit(SessionRuntimeEvent.SEAL_DRAIN_TIMEOUT, null)
            observerJobs.forEach(Job::cancel)
            delay(POST_CANCEL_DELAY_MS)
        }

        close()
        eventSink.emit(SessionRuntimeEvent.SEALED, null)
    }

    suspend fun close() {
        resourceMutex.withLock {
            val current = handle ?: return
            try {
                resource.close(current)
            } catch (error: Throwable) {
                eventSink.emit(SessionRuntimeEvent.CLOSE_FAILED, error)
            } finally {
                handle = null
            }
        }
    }

    private suspend fun acquireLease() {
        leaseMutex.withLock {
            ensureUnlocked()
            activeLeases.incrementAndGet()
        }
    }

    private fun releaseLease() {
        activeLeases.decrementAndGet()
    }

    private fun ensureUnlocked() {
        val state = mutableLockState.value
        if (state != SecureSessionState.UNLOCKED) {
            throw SessionLockedException("Session is ${state.name}")
        }
    }

    private suspend fun resolveHandle(): Handle = resourceMutex.withLock {
        handle ?: throw SessionLockedException("Session resource is not open")
    }
}
