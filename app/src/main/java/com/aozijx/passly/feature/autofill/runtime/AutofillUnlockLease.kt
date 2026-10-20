package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Tracks only vault access acquired by Autofill; pre-existing access is never closed here. */
@Singleton
class AutofillUnlockLease @Inject constructor(
    private val sessionLockController: SessionLockController,
) {
    private val mutex = Mutex()
    private val stateMonitor = Any()
    private val borrowers = mutableSetOf<AutofillInteractionId>()
    private var owned = false
    private var acceptingNewBorrowers = true
    private var completionsInFlight = 0
    private var completionBarrier: CompletableDeferred<Unit>? = null
    private var lockPending = false

    suspend fun awaitReadyForInteraction() {
        while (true) {
            val barrier = synchronized(stateMonitor) { completionBarrier } ?: return
            barrier.await()
        }
    }

    suspend fun acquireOrBorrow(
        interactionId: AutofillInteractionId,
        acquiredUnlock: Boolean,
    ) = mutex.withLock {
        synchronized(stateMonitor) {
            if (acquiredUnlock) {
                owned = true
                if (completionBarrier == null) acceptingNewBorrowers = true
            }
            if (owned) borrowers += interactionId
        }
    }

    suspend fun borrowIfOwned(interactionId: AutofillInteractionId): Boolean = mutex.withLock {
        synchronized(stateMonitor) {
            val accepted = owned && acceptingNewBorrowers
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
            val shouldLock = mutex.withLock {
                synchronized(stateMonitor) {
                    lockPending.also { lockPending = false }
                }
            }
            if (shouldLock) lockAutofillSession()
            return true
        } finally {
            val completedBarrier = synchronized(stateMonitor) {
                acceptingNewBorrowers = true
                completionBarrier.also { completionBarrier = null }
            }
            completedBarrier?.complete(Unit)
        }
    }

    suspend fun release(interactionId: AutofillInteractionId) = mutex.withLock {
        val shouldLock = synchronized(stateMonitor) {
            borrowers -= interactionId
            if (owned && borrowers.isEmpty()) {
                owned = false
                if (completionBarrier == null) {
                    true
                } else {
                    lockPending = true
                    false
                }
            } else {
                false
            }
        }
        if (shouldLock) lockAutofillSession()
    }

    private suspend fun lockAutofillSession() = sessionLockController.lock(
        SessionLockRequest(
            LockReason.AUTOFILL_REQUEST_FINISHED,
            LockTrigger.AUTOFILL_COMPLETION,
        ),
    )
}
