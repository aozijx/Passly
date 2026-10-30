package com.aozijx.passly.runtime.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class SessionLeaseGate<Handle : Any>(
    private val resource: SessionResource<Handle>,
    private val eventSink: SessionEventSink,
) {
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
        val startedAt = elapsedRealtimeMs()
        val fromState = mutableLockState.value
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.UNLOCKED }
        emitTransition(
            startedAt = startedAt,
            fromState = fromState,
            toState = SecureSessionState.UNLOCKED,
            reason = SessionTransitionReason.RESUME,
            lockType = SessionLockType.NONE,
        )
        true
    }

    suspend fun open(key: ByteArray): SessionUnlockResult {
        val startedAt = elapsedRealtimeMs()
        val fromState = mutableLockState.value
        var opened: Handle? = null
        var published = false
        return try {
            val resourceHandle = resource.open(key)
            opened = resourceHandle
            currentCoroutineContext().ensureActive()
            resourceMutex.withLock {
                handle = resourceHandle
                published = true
            }
            currentCoroutineContext().ensureActive()
            leaseMutex.withLock { mutableLockState.value = SecureSessionState.UNLOCKED }
            emitTransition(
                startedAt,
                fromState,
                SecureSessionState.UNLOCKED,
                SessionTransitionReason.OPEN,
                SessionLockType.NONE,
            )
            currentCoroutineContext().ensureActive()
            SessionUnlockResult.Success
        } catch (cancelled: CancellationException) {
            discardOpened(opened, published, cancelled)
            throw cancelled
        } catch (error: Throwable) {
            discardOpened(opened, published, error)
            emitTransition(
                startedAt,
                fromState,
                SecureSessionState.SEALED,
                SessionTransitionReason.OPEN_FAILED,
                SessionLockType.NONE,
                error = error,
            )
            SessionUnlockResult.OpenFailed(error)
        }
    }

    private suspend fun discardOpened(
        opened: Handle?,
        published: Boolean,
        cause: Throwable,
    ) = withContext(NonCancellable) {
        var shouldSeal = opened == null
        if (opened != null) {
            if (published) {
                resourceMutex.withLock {
                    if (handle === opened) {
                        handle = null
                        shouldSeal = true
                        closeAfterFailedOpen(opened, cause)
                    }
                }
            } else {
                shouldSeal = true
                closeAfterFailedOpen(opened, cause)
            }
        }
        if (shouldSeal) {
            leaseMutex.withLock { mutableLockState.value = SecureSessionState.SEALED }
        }
    }

    private suspend fun closeAfterFailedOpen(opened: Handle, cause: Throwable) {
        try {
            resource.close(opened)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== cause) cause.addSuppressed(cleanupFailure)
        }
    }

    suspend fun softLock() {
        val startedAt = elapsedRealtimeMs()
        val fromState = mutableLockState.value
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.SOFT_LOCKED }
        emitTransition(
            startedAt,
            fromState,
            SecureSessionState.SOFT_LOCKED,
            SessionTransitionReason.SOFT_LOCK,
            SessionLockType.SOFT,
        )
    }

    suspend fun seal(timeout: Duration) {
        val startedAt = elapsedRealtimeMs()
        val fromState = mutableLockState.value
        leaseMutex.withLock { mutableLockState.value = SecureSessionState.SEALED }

        val drained = withTimeoutOrNull(timeout) {
            while (activeLeases.get() > 0) delay(DRAIN_POLL_MS)
        }
        if (drained == null) {
            emitTransition(
                startedAt,
                fromState,
                SecureSessionState.SEALED,
                SessionTransitionReason.DRAIN_TIMEOUT,
                SessionLockType.SEALED,
                timeoutMs = timeout.inWholeMilliseconds,
                activeLeaseCount = activeLeases.get(),
            )
            observerJobs.forEach(Job::cancel)
            delay(POST_CANCEL_DELAY_MS)
        }

        close()
        emitTransition(
            startedAt,
            fromState,
            SecureSessionState.SEALED,
            SessionTransitionReason.SEAL,
            SessionLockType.SEALED,
            timeoutMs = timeout.inWholeMilliseconds,
        )
    }

    suspend fun close() {
        val startedAt = elapsedRealtimeMs()
        resourceMutex.withLock {
            val current = handle ?: return
            try {
                resource.close(current)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val state = mutableLockState.value
                emitTransition(
                    startedAt,
                    state,
                    state,
                    SessionTransitionReason.CLOSE_FAILED,
                    if (state == SecureSessionState.SEALED) SessionLockType.SEALED else SessionLockType.NONE,
                    error = error,
                )
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

    private suspend fun emitTransition(
        startedAt: Long,
        fromState: SecureSessionState,
        toState: SecureSessionState,
        reason: SessionTransitionReason,
        lockType: SessionLockType,
        timeoutMs: Long? = null,
        activeLeaseCount: Int = 0,
        error: Throwable? = null,
    ) {
        eventSink.emit(
            SessionTransitionEvent(
                fromState = fromState,
                toState = toState,
                owner = SessionTransitionOwner.SESSION_RUNTIME,
                reason = reason,
                lockType = lockType,
                elapsedMs = (elapsedRealtimeMs() - startedAt).coerceAtLeast(0),
                timeoutMs = timeoutMs,
                activeLeaseCount = activeLeaseCount,
                error = error,
            ),
        )
    }

    private fun elapsedRealtimeMs(): Long = System.nanoTime() / NANOS_PER_MILLISECOND

    private companion object {
        const val DRAIN_POLL_MS = 50L
        const val POST_CANCEL_DELAY_MS = 100L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
