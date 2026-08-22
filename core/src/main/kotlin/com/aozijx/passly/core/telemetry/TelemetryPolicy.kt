package com.aozijx.passly.core.telemetry

import kotlinx.coroutines.flow.Flow

data class TelemetryPolicy(
    val androidSinkEnabled: Boolean = true,
    val encryptedFileEnabledUntilMs: Long = 0L
) {
    fun isEncryptedFileEnabled(nowMs: Long = System.currentTimeMillis()): Boolean =
        encryptedFileEnabledUntilMs > nowMs
}

interface TelemetryPolicyController {
    val policies: Flow<TelemetryPolicy>

    suspend fun enableEncryptedFile(durationMs: Long = DIAGNOSTIC_WINDOW_MS)
    suspend fun disableEncryptedFile()
    suspend fun setAndroidSinkEnabled(enabled: Boolean)

    companion object {
        const val DIAGNOSTIC_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
