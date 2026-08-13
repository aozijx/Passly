package com.aozijx.passly.domain.access.model

data class SessionPolicy(
    val lockOnBackground: Boolean = false,
    val idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT_MS,
) {
    init {
        require(idleTimeoutMs >= 0) { "idleTimeoutMs must not be negative" }
    }

    companion object {
        const val DEFAULT_IDLE_TIMEOUT_MS = 60_000L
    }
}
