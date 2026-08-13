package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface AuthenticationManager {
    val state: StateFlow<AuthenticationState>
    val methods: StateFlow<AuthenticationMethods>

    suspend fun authenticate(
        request: AuthenticationRequest,
        input: AuthInput = AuthInput.Interactive,
    ): AuthenticationResult

    suspend fun lock(reason: LockReason)
    /**
     * 在 [AuthenticationPurpose.RECOVER_DATABASE] 已暂存 DEK 且新数据库已打开后，
     * 发布解锁会话。其他调用顺序必须失败。
     */
    suspend fun completeDatabaseRecovery(): Boolean
    suspend fun refreshAvailability()
    fun snapshot(): AuthenticationSnapshot
}

interface SecureSessionAccessState {
    val authenticationState: StateFlow<AuthenticationState>
    val isAuthorized: Flow<Boolean>
        get() = authenticationState.map { it is AuthenticationState.Authenticated }

    /** 数据库是否已打开（包括恢复模式） */
    fun isDatabaseOpen(): Boolean = isUnlocked()

    /** 是否有完整数据访问权限（Authenticated 且非 RecoveryMode） */
    fun hasFullSecureSessionAccess(): Boolean = isUnlocked() && !isRecoveryMode()

    fun isUnlocked(): Boolean
    fun isRecoveryMode(): Boolean = authenticationState.value is AuthenticationState.RecoveryMode
    fun isLocked(): Boolean = !isUnlocked()
}
