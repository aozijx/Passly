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

    /** 数据库是否已打开（包括恢复模式） */
    fun isDatabaseOpen(): Boolean = isUnlocked()

    /** 是否有完整 Vault 访问权限（Authenticated 且非 RecoveryMode） */
    fun hasFullVaultAccess(): Boolean = isUnlocked() && !isRecoveryMode()

    fun isUnlocked(): Boolean
    fun isRecoveryMode(): Boolean = authenticationState.value is AuthenticationState.RecoveryMode
    fun isLocked(): Boolean = !isUnlocked()
}
