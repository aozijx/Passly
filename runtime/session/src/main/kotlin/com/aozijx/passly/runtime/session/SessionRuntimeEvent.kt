package com.aozijx.passly.runtime.session

enum class SessionRuntimeEvent {
    OPENED,
    OPEN_FAILED,
    RESUMED,
    SOFT_LOCKED,
    SEAL_DRAIN_TIMEOUT,
    SEALED,
    CLOSE_FAILED,
}

fun interface SessionEventSink {
    fun emit(event: SessionRuntimeEvent, error: Throwable?)

    companion object {
        val None = SessionEventSink { _, _ -> }
    }
}
