package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton
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

    suspend fun acquireOrBorrow(
        interactionId: AutofillInteractionId,
        acquiredUnlock: Boolean,
    ) = mutex.withLock {
        synchronized(stateMonitor) {
            if (acquiredUnlock) {
                owned = true
                acceptingNewBorrowers = true
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

    /** Prevents later Autofill requests from inheriting a session whose result was delivered. */
    fun stopAcceptingNewBorrowers(interactionId: AutofillInteractionId) {
        synchronized(stateMonitor) {
            if (owned && interactionId in borrowers) acceptingNewBorrowers = false
        }
    }

    suspend fun release(interactionId: AutofillInteractionId) = mutex.withLock {
        val shouldLock = synchronized(stateMonitor) {
            borrowers -= interactionId
            if (owned && borrowers.isEmpty()) {
                owned = false
                acceptingNewBorrowers = true
                true
            } else {
                false
            }
        }
        if (shouldLock) {
            sessionLockController.lock(
                SessionLockRequest(
                    LockReason.AUTOFILL_REQUEST_FINISHED,
                    LockTrigger.AUTOFILL_COMPLETION,
                ),
            )
        }
    }
}
