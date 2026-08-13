package com.aozijx.passly.security.lock

import com.aozijx.passly.runtime.session.SecureSessionState
import javax.inject.Inject
import javax.inject.Singleton

/** Owns only the in-memory lock strength; lock side effects remain in the session controller. */
@Singleton
class LockStateManager @Inject constructor() {
    @Volatile
    var state: SecureSessionState = SecureSessionState.SEALED
        private set

    fun isUnlocked(): Boolean = state == SecureSessionState.UNLOCKED

    fun isLocked(): Boolean = !isUnlocked()

    fun mark(newState: SecureSessionState) {
        state = newState
    }
}
