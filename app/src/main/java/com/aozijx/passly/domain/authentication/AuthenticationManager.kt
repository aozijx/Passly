package com.aozijx.passly.domain.authentication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface AuthenticationManager {
    val state: StateFlow<AuthenticationState>
    val methods: StateFlow<AuthMethodAvailability>
    val databaseFailure: StateFlow<Throwable?>

    suspend fun authenticate(
        request: AuthenticationRequest,
        credential: CharArray? = null
    ): AuthenticationResult

    fun authenticate(
        request: AuthenticationRequest,
        callback: AuthenticationCallback
    ): AuthenticationRequestHandle

    suspend fun lock(reason: LockReason)
    /**
     * 在 [AuthenticationPurpose.RECOVER_DATABASE] 已暂存 DEK 且新数据库已打开后，
     * 发布解锁会话。其他调用顺序必须失败。
     */
    suspend fun completeDatabaseRecovery(): Boolean
    fun clearDatabaseFailure()
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
