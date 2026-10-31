package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Tracks only vault access acquired by Autofill; pre-existing access is never closed here. */
@Singleton
class AutofillUnlockLease private constructor(
    private val sessionLockController: SessionLockController,
    private val retryScope: CoroutineScope,
) {
    @Inject
    constructor(sessionLockController: SessionLockController) : this(
        sessionLockController,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    internal constructor(
        sessionLockController: SessionLockController,
        retryScope: CoroutineScope,
        @Suppress("UNUSED_PARAMETER") testScope: Unit,
    ) : this(sessionLockController, retryScope)

    private val mutex = Mutex()
    private val stateMonitor = Any()
    private val borrowers = mutableSetOf<AutofillInteractionId>()
    private var owned = false
    private var acceptingNewBorrowers = true
    private var completionsInFlight = 0
    private var completionBarrier: CompletableDeferred<Unit>? = null
    private var lockPending = false
    private var lockRetryJob: Job? = null

    suspend fun awaitReadyForInteraction() {
        while (true) {
            val barrier = synchronized(stateMonitor) { completionBarrier }
            if (barrier != null) {
                barrier.await()
                continue
            }
            retryPendingLock()
            if (synchronized(stateMonitor) { completionBarrier } == null) return
        }
    }

    suspend fun acquireOrBorrow(
        interactionId: AutofillInteractionId,
        acquiredUnlock: Boolean,
    ) = mutex.withLock {
        synchronized(stateMonitor) {
            if (acquiredUnlock) {
                owned = true
                lockPending = false
                if (completionBarrier == null) acceptingNewBorrowers = true
            }
            if (owned && !lockPending) borrowers += interactionId
        }
    }

    suspend fun borrowIfOwned(interactionId: AutofillInteractionId): Boolean = mutex.withLock {
        synchronized(stateMonitor) {
            val accepted = owned && !lockPending && acceptingNewBorrowers
            if (accepted) borrowers += interactionId
            accepted
        }
    }

    /** Opens a completion barrier before the delivered interaction starts closing its session. */
    fun beginResultCompletion(interactionId: AutofillInteractionId) {
        synchronized(stateMonitor) {
            if (completionsInFlight == 0) completionBarrier = CompletableDeferred()
            completionsInFlight += 1
            if (owned && interactionId in borrowers) acceptingNewBorrowers = false
        }
    }

    suspend fun endResultCompletion(finalizeSession: suspend () -> Unit): Boolean {
        val isLastCompletion = synchronized(stateMonitor) {
            check(completionsInFlight > 0) { "No Autofill result completion is active" }
            completionsInFlight -= 1
            completionsInFlight == 0
        }
        if (!isLastCompletion) return false

        try {
            finalizeSession()
            retryPendingLock(allowLastCompletionBarrier = true)
            return true
        } finally {
            val (completedBarrier, shouldScheduleRetry) = synchronized(stateMonitor) {
                acceptingNewBorrowers = !lockPending
                val barrier = completionBarrier.also { completionBarrier = null }
                barrier to (lockPending && owned && borrowers.isEmpty())
            }
            completedBarrier?.complete(Unit)
            if (shouldScheduleRetry) schedulePendingLockRetry()
        }
    }

    suspend fun release(interactionId: AutofillInteractionId) = withContext(NonCancellable) {
        mutex.withLock {
            val shouldLock = synchronized(stateMonitor) {
                borrowers -= interactionId
                if (owned && borrowers.isEmpty()) {
                    lockPending = true
                    acceptingNewBorrowers = false
                    completionBarrier == null
                } else {
                    false
                }
            }
            if (shouldLock) lockOwnedSessionIfPendingLocked()
        }
    }

    private suspend fun retryPendingLock(allowLastCompletionBarrier: Boolean = false) =
        withContext(NonCancellable) {
            mutex.withLock {
                lockOwnedSessionIfPendingLocked(allowLastCompletionBarrier)
            }
    }

    /** Keep ownership until lock() returns; failures leave a retryable lock obligation. */
    private suspend fun lockOwnedSessionIfPendingLocked(
        allowLastCompletionBarrier: Boolean = false,
    ) {
        val shouldLock = synchronized(stateMonitor) {
            lockPending && owned && borrowers.isEmpty() &&
                (completionBarrier == null || allowLastCompletionBarrier && completionsInFlight == 0)
        }
        if (!shouldLock) return

        try {
            lockAutofillSession()
        } catch (failure: Exception) {
            schedulePendingLockRetry()
            throw failure
        }
        synchronized(stateMonitor) {
            if (lockPending && borrowers.isEmpty() &&
                (completionBarrier == null || allowLastCompletionBarrier && completionsInFlight == 0)
            ) {
                owned = false
                lockPending = false
                acceptingNewBorrowers = true
                lockRetryJob?.cancel()
                lockRetryJob = null
            }
        }
    }

    /** Process-owned retry survives page teardown and never holds the lease mutex while backing off. */
    private fun schedulePendingLockRetry() {
        synchronized(stateMonitor) {
            if (lockRetryJob?.isActive == true) return
            lockRetryJob = retryScope.launch(start = CoroutineStart.DEFAULT) {
                var delayMs = FIRST_LOCK_RETRY_DELAY_MS
                while (true) {
                    delay(delayMs)
                    val shouldRetry = synchronized(stateMonitor) {
                        lockPending && owned && borrowers.isEmpty() && completionBarrier == null
                    }
                    if (!shouldRetry) return@launch

                    try {
                        retryPendingLock()
                        val stillPending = synchronized(stateMonitor) { lockPending }
                        if (!stillPending) return@launch
                    } catch (_: Exception) {
                        delayMs = (delayMs * 2).coerceAtMost(MAX_LOCK_RETRY_DELAY_MS)
                    }
                }
            }.also { job ->
                job.invokeOnCompletion {
                    synchronized(stateMonitor) {
                        if (lockRetryJob === job) lockRetryJob = null
                    }
                }
            }
        }
    }

    private suspend fun lockAutofillSession() = sessionLockController.lock(
        SessionLockRequest(
            LockReason.AUTOFILL_REQUEST_FINISHED,
            LockTrigger.AUTOFILL_COMPLETION,
        ),
    )

    private companion object {
        const val FIRST_LOCK_RETRY_DELAY_MS = 1_000L
        const val MAX_LOCK_RETRY_DELAY_MS = 60_000L
    }
}
