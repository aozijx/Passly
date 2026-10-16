package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.SessionLockRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

interface AuthenticationManager : AuthenticationRequester, AuthenticationMethodAvailability, SessionLockController {
    val state: StateFlow<AuthenticationState>
    override suspend fun lock(request: SessionLockRequest)
    suspend fun refreshAvailability()
    fun snapshot(): AuthenticationSnapshot
}

interface AuthenticationRequester {
    suspend fun authenticate(
        request: AuthenticationRequest,
        input: AuthInput = AuthInput.Interactive,
    ): AuthenticationResult
}

interface SessionLockController {
    suspend fun lock(request: SessionLockRequest)
}

interface SecureSessionAccessState {
    val authenticationState: StateFlow<AuthenticationState>
    val isAuthorized: Flow<Boolean>
        get() = authenticationState.map { it is AuthenticationState.Authenticated }

    /** 数据库是否已打开（包括恢复模式） */
    fun isDatabaseOpen(): Boolean = isUnlocked()
    /** 是否有主应用完整数据访问权限。物理存储仍打开不代表认证状态仍可授权。 */
    fun hasFullSecureSessionAccess(): Boolean =
        authenticationState.value is AuthenticationState.Authenticated && isUnlocked()

    /** Whether the open database session may serve Autofill without exposing the Vault UI. */
    fun hasAutofillSessionAccess(): Boolean = when (authenticationState.value) {
        is AuthenticationState.Authenticated,
        is AuthenticationState.AutofillAuthenticated -> isUnlocked()
        else -> false
    }

    fun isUnlocked(): Boolean
    fun isRecoveryMode(): Boolean = authenticationState.value is AuthenticationState.RecoveryMode
    fun isLocked(): Boolean = !isUnlocked()
}
