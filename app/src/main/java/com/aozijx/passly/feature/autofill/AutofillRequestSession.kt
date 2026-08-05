package com.aozijx.passly.feature.autofill

import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Owns the temporary vault unlock created by one platform autofill request.
 *
 * An autofill authentication Activity can finish without a Dataset being
 * selected (back gesture, empty result, stale entry, or a platform timeout).
 * In all of those paths the unlock must not leak into the next request.
 *
 * A session that started while the vault was already unlocked never locks it.
 */
class AutofillRequestSession @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: VaultAccessState,
) {
    private val closeMutex = Mutex()

    @Volatile
    private var ownsUnlock = false

    suspend fun authenticate(): AuthenticationResult = trackUnlock {
        authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
        )
    }

    suspend fun <T> trackUnlock(block: suspend () -> T): T {
        val startedLocked = !vaultAccessState.hasFullVaultAccess()
        return try {
            block()
        } finally {
            if (startedLocked && vaultAccessState.isUnlocked()) {
                ownsUnlock = true
            }
        }
    }

    suspend fun close() {
        closeMutex.withLock {
            if (!ownsUnlock) return
            ownsUnlock = false
            authenticationManager.lock(LockReason.AUTOFILL_REQUEST_FINISHED)
        }
    }
}
