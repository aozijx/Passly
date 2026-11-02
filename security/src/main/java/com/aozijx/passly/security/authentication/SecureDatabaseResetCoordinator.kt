package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.DatabaseResetSessionCoordinator
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.security.dek.SensitiveDataKeyClearer
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
    private val sensitiveDataKeyClearer: SensitiveDataKeyClearer,
    private val authenticationState: MutableStateFlow<AuthenticationState>,
    private val databaseFailure: MutableStateFlow<Throwable?>,
) : DatabaseResetSessionCoordinator {
    override suspend fun withExclusiveDatabaseReset(operation: suspend () -> Throwable?): Throwable? {
        var fatalForCaller: Error? = null
        return try {
            sessionMutex.withLock {
                if (!hasSecureAccess()) {
                    return@withLock SessionLockedException("Database reset requires an unlocked Vault session")
                }

                lockStateManager.mark(SecureSessionState.SEALED)
                var operationFailure: Throwable? = null
                var fatalError: Error? = null
                try {
                    operation().also {
                        operationFailure = it
                        fatalError = it as? Error
                    }
                } catch (error: Throwable) {
                    operationFailure = error
                    fatalError = error as? Error
                    throw error
                } finally {
                    withContext(NonCancellable) {
                        val failure =
                            operationFailure
                                ?: SessionLockedException("Database reset did not reopen the secure database")

                        fun collectCleanupFailure(cleanupFailure: Throwable) {
                            when (cleanupFailure) {
                                is Error -> {
                                    when (val primaryFatal = fatalError) {
                                        null -> {
                                            fatalError = cleanupFailure
                                            if (failure !== cleanupFailure) cleanupFailure.addSuppressed(failure)
                                        }

                                        else -> {
                                            if (primaryFatal !== cleanupFailure) {
                                                primaryFatal.addSuppressed(cleanupFailure)
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    if (failure !== cleanupFailure) failure.addSuppressed(cleanupFailure)
                                }
                            }
                        }

                        fatalError?.takeIf { sessionManager.lockState != SecureSessionState.SEALED }?.let {
                            try {
                                sessionManager.seal().exceptionOrNull()?.let(::collectCleanupFailure)
                            } catch (cleanupFailure: Throwable) {
                                collectCleanupFailure(cleanupFailure)
                            }
                        }

                        when {
                            sessionManager.lockState == SecureSessionState.UNLOCKED && fatalError == null -> {
                                lockStateManager.mark(SecureSessionState.UNLOCKED)
                                databaseFailure.value = null
                            }

                            else -> {
                                lockStateManager.mark(sessionManager.lockState)
                                try {
                                    authorizationPermitRevoker.revokeAll()
                                } catch (cleanupFailure: Throwable) {
                                    collectCleanupFailure(cleanupFailure)
                                }
                                try {
                                    sensitiveDataKeyClearer.clear()
                                } catch (cleanupFailure: Throwable) {
                                    collectCleanupFailure(cleanupFailure)
                                }
                                withContext(Dispatchers.Main.immediate) {
                                    authenticationState.value = AuthenticationState.Locked
                                }
                                databaseFailure.value = fatalError ?: failure
                                fatalError?.let {
                                    fatalForCaller = it
                                    throw it
                                }
                            }
                        }
                    }
                }
            }
        } catch (failure: Throwable) {
            fatalForCaller?.let { throw it }
            throw failure
        }
    }

    private fun hasSecureAccess(): Boolean =
        authenticationState.value is AuthenticationState.Authenticated &&
            lockStateManager.isUnlocked() &&
            sessionManager.lockState == SecureSessionState.UNLOCKED
}
