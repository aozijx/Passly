package com.aozijx.passly.domain.access.port

fun interface DatabaseSessionRecovery {
    suspend fun retry(): DatabaseSessionRetryResult
}

sealed interface DatabaseSessionRetryResult {
    data object Ready : DatabaseSessionRetryResult
    data object Unavailable : DatabaseSessionRetryResult
    data class Failed(val cause: Throwable) : DatabaseSessionRetryResult
}