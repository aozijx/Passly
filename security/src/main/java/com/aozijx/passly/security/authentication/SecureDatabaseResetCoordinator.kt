package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.DatabaseResetSessionCoordinator
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.security.lock.LockStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serializes reset file work with security lock/unlock transitions and reconciles the owner state. */
internal class SecureDatabaseResetCoordinator(
    private val sessionMutex: Mutex,
    private val sessionManager: DatabaseSessionLifecycle,
    private val lockStateManager: LockStateManager,
    private val authorizationPermitRevoker: AuthorizationPermitRevoker,
    private val sensitiveDataKeyManager: SensitiveDataKeyManager,
    private val authenticationState: MutableStateFlow<AuthenticationState>,
    private val databaseFailure: MutableStateFlow<Throwable?>,
) : DatabaseResetSessionCoordinator {
    override suspend fun withExclusiveDatabaseReset(
        operation: suspend () -> Throwable?,
    ): Throwable? = sessionMutex.withLock {
        if (!hasSecureAccess()) {
            return@withLock SessionLockedException("Database reset requires an unlocked Vault session")
        }

        lockStateManager.mark(SecureSessionState.SEALED)
        var operationFailure: Throwable? = null
        try {
            operation().also { operationFailure = it }
        } catch (error: Throwable) {
            operationFailure = error
            throw error
        } finally {
            withContext(NonCancellable) {
                if (sessionManager.lockState == SecureSessionState.UNLOCKED) {
                    lockStateManager.mark(SecureSessionState.UNLOCKED)
                    databaseFailure.value = null
                } else {
                    val failure = operationFailure
                        ?: SessionLockedException("Database reset did not reopen the secure database")
                    databaseFailure.value = failure
                    lockStateManager.mark(sessionManager.lockState)
                    runCatching { authorizationPermitRevoker.revokeAll() }
                        .exceptionOrNull()
                        ?.let(failure::addSuppressed)
                    runCatching { sensitiveDataKeyManager.clear() }
                        .exceptionOrNull()
                        ?.let(failure::addSuppressed)
                    withContext(Dispatchers.Main.immediate) {
                        authenticationState.value = AuthenticationState.Locked
                    }
                }
            }
        }
    }

    private fun hasSecureAccess(): Boolean =
        authenticationState.value is AuthenticationState.Authenticated &&
            lockStateManager.isUnlocked() &&
            sessionManager.lockState == SecureSessionState.UNLOCKED
}
