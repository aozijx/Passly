package com.aozijx.passly.domain.settings.model

data class ClipboardClearPolicy(
    val enabled: Boolean = true,
    val delaySeconds: Int = DEFAULT_DELAY_SECONDS,
) {
    companion object {
        const val DEFAULT_DELAY_SECONDS: Int = 30
        val ALLOWED_DELAY_SECONDS: Set<Int> = linkedSetOf(15, 30, 60, 120)

        fun normalizeDelaySeconds(value: Int): Int =
            value.takeIf(ALLOWED_DELAY_SECONDS::contains) ?: DEFAULT_DELAY_SECONDS
    }
}
