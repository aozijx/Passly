package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.lock.LockStateManager

internal class DatabaseSessionRetryCoordinator(
    private val sessionManager: DatabaseSessionLifecycle,
    private val lockStateManager: LockStateManager,
    private val currentFailure: () -> Throwable?,
    private val updateFailure: (Throwable?) -> Unit,
    private val publishAuthenticated: suspend () -> Unit,
) {
    suspend fun retry(): DatabaseSessionRetryResult {
        if (currentFailure() == null) return DatabaseSessionRetryResult.Unavailable

        val error = sessionManager.unlock()
        if (error != null) {
            updateFailure(error)
            return DatabaseSessionRetryResult.Failed(error)
        }

        updateFailure(null)
        lockStateManager.mark(SecureSessionState.UNLOCKED)
        publishAuthenticated()
        return DatabaseSessionRetryResult.Ready
    }
}