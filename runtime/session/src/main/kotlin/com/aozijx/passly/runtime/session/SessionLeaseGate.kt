package com.aozijx.passly.runtime.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutableLockState = MutableStateFlow(SecureSessionState.SEALED)
    val lockState: StateFlow<SecureSessionState> = mutableLockState.asStateFlow()

    private val activeLeases = AtomicInteger(0)
    private val leaseOwners = ConcurrentHashMap<Job, Int>()
    private val leaseMutex = Mutex()
    private val resourceMutex = Mutex()

    private var handle: Handle? = null
    private var closeFailure: Throwable? = null

    suspend fun <T> read(block: suspend Handle.() -> T): T {
        val owner = acquireLease()
        return try {
            resolveHandle().block()
        } finally {
            releaseLease(owner)
        }
    }

    suspend fun <T> write(block: suspend Handle.() -> T): T {
        val owner = acquireLease()
        return try {
            val current = resolveHandle()
            resource.transaction(current, block)
        } finally {
            releaseLease(owner)
        }
    }

    fun <T> observe(block: suspend Handle.() -> Flow<T>): Flow<T> = flow {
        val owner = acquireLease()
        try {
            resolveHandle().block().collect { value ->
                ensureUnlocked()
                emit(value)
            }
        } finally {
            releaseLease(owner)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resumeIfOpen(): Boolean = resourceMutex.withLock {
        if (closeFailure != null) return false
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
        closeFailure()?.let { return SessionUnlockResult.OpenFailed(it) }
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
            discardOpened(opened, published, cancelled)?.let { throw it }
            throw cancelled
        } catch (error: Error) {
            discardOpened(opened, published, error)
            throw error
        } catch (error: Exception) {
            discardOpened(opened, published, error)?.let { throw it }
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
    ): Error? = withContext(NonCancellable) {
        var shouldSeal = opened == null
        var fatalCleanup: Error? = null
        try {
            if (opened != null) {
                if (published) {
                    resourceMutex.withLock {
                        if (handle === opened) {
                            handle = null
                            shouldSeal = true
                            fatalCleanup = closeAfterFailedOpen(opened, cause)
                        }
                    }
                } else {
                    shouldSeal = true
                    fatalCleanup = closeAfterFailedOpen(opened, cause)
                }
            }
        } finally {
            if (shouldSeal) {
                leaseMutex.withLock { mutableLockState.value = SecureSessionState.SEALED }
            }
        }
        fatalCleanup
    }

    private suspend fun closeAfterFailedOpen(opened: Handle, cause: Throwable) =
        withContext(ioDispatcher) {
            try {
                resource.close(opened)
                null
            } catch (cleanupFailure: Error) {
                if (cause is Error) {
                    if (cleanupFailure !== cause) cause.addSuppressed(cleanupFailure)
                    null
                } else {
                    if (cause !== cleanupFailure) cleanupFailure.addSuppressed(cause)
                    cleanupFailure
                }
            } catch (cleanupFailure: Exception) {
                if (cleanupFailure !== cause) cause.addSuppressed(cleanupFailure)
                null
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

    suspend fun seal(timeout: Duration): Result<Unit> {
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
            leaseOwners.keys.forEach(Job::cancel)
            withTimeoutOrNull(POST_CANCEL_GRACE_MS) {
                while (activeLeases.get() > 0) delay(DRAIN_POLL_MS)
            }
        }

        val closeResult = close()
        emitTransition(
            startedAt,
            fromState,
            SecureSessionState.SEALED,
            SessionTransitionReason.SEAL,
            SessionLockType.SEALED,
            timeoutMs = timeout.inWholeMilliseconds,
        )
        return closeResult
    }

    suspend fun closeFailure(): Throwable? = resourceMutex.withLock { closeFailure }

    private suspend fun close(): Result<Unit> {
        val startedAt = elapsedRealtimeMs()
        return resourceMutex.withLock {
            closeFailure?.let { return@withLock Result.failure(it) }
            val current = handle ?: return@withLock Result.success(Unit)
            var fatalClose: Error? = null
            try {
                withContext(ioDispatcher) {
                    try {
                        resource.close(current)
                    } catch (error: Error) {
                        fatalClose = error
                        throw error
                    }
                }
                handle = null
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                closeFailure = cancelled
                throw cancelled
            } catch (error: Throwable) {
                val closeError = fatalClose ?: error
                closeFailure = closeError
                val state = mutableLockState.value
                try {
                    emitTransition(
                        startedAt,
                        state,
                        state,
                        SessionTransitionReason.CLOSE_FAILED,
                        if (state == SecureSessionState.SEALED) SessionLockType.SEALED else SessionLockType.NONE,
                        error = closeError,
                    )
                } catch (eventError: Throwable) {
                    fatalClose?.let {
                        if (it !== eventError) it.addSuppressed(eventError)
                        throw it
                    }
                    throw eventError
                }
                fatalClose?.let { throw it }
                if (error is Error) throw error
                Result.failure(closeError)
            }
        }
    }

    private suspend fun acquireLease(): Job? {
        val owner = currentCoroutineContext()[Job]
        leaseMutex.withLock {
            ensureUnlocked()
            activeLeases.incrementAndGet()
            if (owner != null) leaseOwners.compute(owner) { _, count -> (count ?: 0) + 1 }
        }
        return owner
    }

    private fun releaseLease(owner: Job?) {
        activeLeases.decrementAndGet()
        if (owner != null) {
            leaseOwners.computeIfPresent(owner) { _, count -> if (count == 1) null else count - 1 }
        }
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
        const val POST_CANCEL_GRACE_MS = 100L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
