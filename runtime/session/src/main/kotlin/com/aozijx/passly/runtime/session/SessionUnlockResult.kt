package com.aozijx.passly.runtime.session

sealed interface SessionUnlockResult {
    data object Success : SessionUnlockResult

    data class KeyUnavailable(val cause: Throwable) : SessionUnlockResult

    data class OpenFailed(val cause: Throwable) : SessionUnlockResult
}
