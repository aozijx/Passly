package com.aozijx.passly.domain.authentication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface AuthenticationManager {
    val state: StateFlow<AuthenticationState>
    val methods: StateFlow<AuthMethodAvailability>

    suspend fun authenticate(
        request: AuthenticationRequest,
        credential: CharArray? = null
    ): AuthenticationResult

    fun authenticate(
        request: AuthenticationRequest,
        callback: AuthenticationCallback
    ): AuthenticationRequestHandle

    suspend fun lock(reason: LockReason)
    suspend fun refreshAvailability()
    fun snapshot(): AuthenticationSnapshot
    fun onUserInteraction()
}

interface VaultAccessState {
    val authenticationState: StateFlow<AuthenticationState>
    val isAuthorized: Flow<Boolean>
        get() = authenticationState.map { it is AuthenticationState.Authenticated }
    fun isUnlocked(): Boolean
    fun isLocked(): Boolean = !isUnlocked()
}
