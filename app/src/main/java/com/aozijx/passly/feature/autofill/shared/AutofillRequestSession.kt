package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the temporary vault unlock created by one platform autofill request.
 *
 * It manages an inactivity timeout (60s) to ensure the vault is locked if the
 * activity is abandoned without a clean exit.
 */
class AutofillRequestSession(
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: SecureSessionAccessState,
    private val grantStore: AutofillGrantStore,
    private val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val closeMutex = Mutex()
    private var timeoutJob: Job? = null

    @Volatile
    private var ownsUnlock = false

    suspend fun authenticate(): AuthenticationResult {
        resetTimeout()
        return trackUnlock {
            authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
            )
        }
    }

    suspend fun <T> trackUnlock(block: suspend () -> T): T {
        val startedLocked = !vaultAccessState.hasFullSecureSessionAccess()
        return try {
            block()
        } finally {
            if (startedLocked && vaultAccessState.hasFullSecureSessionAccess()) {
                ownsUnlock = true
                resetTimeout()
            }
        }
    }

    fun grant(context: AutofillGrantContext) = grantStore.grant(context)

    fun isGranted(context: AutofillGrantContext): Boolean = grantStore.isGranted(context)

    /**
     * Resets the 60-second inactivity timer.
     */
    private fun resetTimeout() {
        timeoutJob?.cancel()
        timeoutJob = sessionScope.launch {
            delay(SESSION_TIMEOUT_MS)
            close()
        }
    }

    /**
     * Closes the session and locks the vault if this session was responsible for the unlock.
     */
    suspend fun close() {
        closeMutex.withLock {
            timeoutJob?.cancel()
            timeoutJob = null
            grantStore.clear()
            if (ownsUnlock) {
                ownsUnlock = false
                authenticationManager.lock(LockReason.AUTOFILL_REQUEST_FINISHED)
            }
        }
    }

    /** Runs cleanup on the session-owned scope after a ViewModel owner is cleared. */
    fun closeOnOwnerCleared() {
        sessionScope.launch { close() }
    }

    companion object {
        private const val SESSION_TIMEOUT_MS = 60_000L
    }
}
