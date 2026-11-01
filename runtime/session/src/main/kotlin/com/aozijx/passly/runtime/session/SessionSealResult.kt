package com.aozijx.passly.runtime.session

/** Result of sealing the session's backing resource. A close failure leaves it permanently sealed in-process. */
sealed interface SessionSealResult {
    data object Sealed : SessionSealResult
    data class CloseFailed(val cause: Throwable) : SessionSealResult
}
