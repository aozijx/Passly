package com.aozijx.passly.domain.authentication

/**
 * Authentication-facing lifecycle contract for the encrypted database session.
 *
 * It deliberately exposes no Room handle, DAO, query, or transaction API.
 */
interface DatabaseSessionLifecycle {
    val lockState: SecureSessionState

    suspend fun unlock(): Throwable?

    suspend fun softLock()

    suspend fun seal()
}
