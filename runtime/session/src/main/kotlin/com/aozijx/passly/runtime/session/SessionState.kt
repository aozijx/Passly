package com.aozijx.passly.runtime.session

import kotlinx.coroutines.flow.StateFlow

enum class SecureSessionState(val strength: Int) {
    UNLOCKED(0),
    SOFT_LOCKED(1),
    SEALED(2);

    infix fun shouldEscalateTo(target: SecureSessionState): Boolean =
        target.strength > strength
}

class SessionLockedException(message: String = "Secure session is locked") :
    IllegalStateException(message)

interface SessionStateProvider {
    val lockState: SecureSessionState
    val lockStateFlow: StateFlow<SecureSessionState>

    val isWritable: Boolean
        get() = lockState == SecureSessionState.UNLOCKED
}

interface DatabaseSessionLifecycle {
    val lockState: SecureSessionState

    suspend fun unlock(): Throwable?
    suspend fun softLock()
    suspend fun seal()
}
