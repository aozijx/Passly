package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.access.model.LockReason
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
    private val borrowers = mutableSetOf<AutofillInteractionId>()
    private var owned = false

    suspend fun acquireOrBorrow(
        interactionId: AutofillInteractionId,
        acquiredUnlock: Boolean,
    ) = mutex.withLock {
        if (acquiredUnlock) owned = true
        if (owned) borrowers += interactionId
    }

    suspend fun borrowIfOwned(interactionId: AutofillInteractionId): Boolean = mutex.withLock {
        if (owned) borrowers += interactionId
        owned
    }

    suspend fun release(interactionId: AutofillInteractionId) = mutex.withLock {
        borrowers -= interactionId
        if (owned && borrowers.isEmpty()) {
            owned = false
            sessionLockController.lock(LockReason.AUTOFILL_REQUEST_FINISHED)
        }
    }
}
