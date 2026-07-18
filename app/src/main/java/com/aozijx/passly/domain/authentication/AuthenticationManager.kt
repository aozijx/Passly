package com.aozijx.passly.domain.authentication

import kotlinx.coroutines.flow.StateFlow

interface AuthenticationManager {
    val state: StateFlow<AuthenticationState>
    val methods: StateFlow<AuthMethodAvailability>

    suspend fun authenticate(request: AuthenticationRequest): AuthenticationResult

    fun authenticate(
        request: AuthenticationRequest,
        callback: AuthenticationCallback
    ): AuthenticationRequestHandle

    suspend fun lock(reason: LockReason)
    fun snapshot(): AuthenticationSnapshot
    fun onUserInteraction()
}

interface VaultAccessState {
    val authenticationState: StateFlow<AuthenticationState>
    fun isUnlocked(): Boolean
}

interface VaultResourceController {
    suspend fun blockNewAccess()
    suspend fun closeAndAwait()
    suspend fun allowAccess()
}
