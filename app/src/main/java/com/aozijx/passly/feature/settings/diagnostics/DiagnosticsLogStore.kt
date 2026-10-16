package com.aozijx.passly.feature.settings.diagnostics

import com.aozijx.passly.core.observability.LogLevel

data class DiagnosticsLogLine(
    val level: LogLevel,
    val text: String,
    val traceId: String,
    val event: String,
    val errorCode: String? = null,
    val exceptionKind: String? = null,
    val exceptionType: String? = null,
)

/** Feature-facing access to the persisted diagnostics log. */
interface DiagnosticsLogStore {
    suspend fun readLines(limit: Int): List<DiagnosticsLogLine>
    suspend fun storageBytes(): Long
    suspend fun clear()
}
