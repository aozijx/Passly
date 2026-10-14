package com.aozijx.passly.runtime.session

enum class SessionTransitionOwner { SESSION_RUNTIME }

enum class SessionTransitionReason {
    OPEN,
    OPEN_FAILED,
    RESUME,
    SOFT_LOCK,
    SEAL,
    DRAIN_TIMEOUT,
    CLOSE_FAILED,
}

enum class SessionLockType { NONE, SOFT, SEALED }

data class SessionTransitionEvent(
    val fromState: SecureSessionState,
    val toState: SecureSessionState,
    val owner: SessionTransitionOwner,
    val reason: SessionTransitionReason,
    val lockType: SessionLockType,
    val elapsedMs: Long,
    val timeoutMs: Long? = null,
    val activeLeaseCount: Int = 0,
    val error: Throwable? = null,
) {
    init {
        require(elapsedMs >= 0) { "Elapsed time cannot be negative" }
        require(timeoutMs == null || timeoutMs >= 0) { "Timeout cannot be negative" }
        require(activeLeaseCount >= 0) { "Active lease count cannot be negative" }
    }
}

fun interface SessionEventSink {
    suspend fun emit(event: SessionTransitionEvent)

    companion object {
        val None = SessionEventSink { }
    }
}
