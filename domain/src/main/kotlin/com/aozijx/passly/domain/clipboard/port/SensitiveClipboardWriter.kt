package com.aozijx.passly.domain.clipboard.port

/** Writes a sensitive value using the active clipboard retention policy. */
fun interface SensitiveClipboardWriter {
    suspend fun writeSensitive(text: String)

    /**
     * Checks the caller's short-lived authorization before writing. Implementations that suspend
     * before the platform side effect must override this method and check again after suspension.
     */
    suspend fun writeSensitiveIf(text: String, canWrite: () -> Boolean): Boolean {
        if (!canWrite()) return false
        writeSensitive(text)
        return true
    }
}
