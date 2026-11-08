package com.aozijx.passly.domain.access.model

data class AuthenticationAttemptState(
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long = 0L,
    val lockedUntilElapsedRealtimeMs: Long = 0L,
    val bootCount: Int = -1,
) {
    init {
        require(failedAttempts >= 0) { "Failed attempts cannot be negative" }
        require(lockedUntilEpochMs >= 0L) { "Wall-clock lockout time cannot be negative" }
        require(lockedUntilElapsedRealtimeMs >= 0L) { "Elapsed lockout time cannot be negative" }
    }
}

data class AuthenticationAttemptStatus(
    val attempts: AttemptStatus,
    val retryAfterMs: Long? = null,
) {
    init {
        require(retryAfterMs == null || retryAfterMs >= 0L) { "Retry delay cannot be negative" }
    }
}
