package com.aozijx.passly.core.diagnostics

import kotlinx.coroutines.flow.Flow

data class DiagnosticsPolicy(
    val androidSinkEnabled: Boolean = true,
    val fileLoggingEnabledUntilMs: Long = 0L
) {
    fun isFileLoggingEnabled(nowMs: Long = System.currentTimeMillis()): Boolean =
        fileLoggingEnabledUntilMs > nowMs
}

interface DiagnosticsPolicyController {
    val policies: Flow<DiagnosticsPolicy>

    suspend fun enableFileLogging(durationMs: Long = DIAGNOSTIC_WINDOW_MS)
    suspend fun disableFileLogging()
    suspend fun setAndroidSinkEnabled(enabled: Boolean)

    companion object {
        const val DIAGNOSTIC_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
