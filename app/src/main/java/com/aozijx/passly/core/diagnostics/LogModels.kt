package com.aozijx.passly.core.diagnostics

import java.util.UUID

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL }

enum class LogCategory {
    APPLICATION,
    AUTHENTICATION,
    SECURITY,
    DATABASE,
    BACKUP,
    AUTOFILL,
    NETWORK,
    FILE_IO,
    UI
}

data class LogEvent(
    val level: LogLevel,
    val category: LogCategory,
    val name: String,
    val fields: Map<String, String> = emptyMap(),
    val throwable: Throwable? = null,
    val correlationId: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis()
)

data class SanitizedLogEvent(
    val level: LogLevel,
    val category: LogCategory,
    val name: String,
    val fields: Map<String, String>,
    val throwableType: String?,
    val appStackFrames: List<String>,
    val correlationId: String,
    val timestampMs: Long
) {
    fun encodeLine(): String = buildString {
        append(timestampMs)
        append('|').append(level.name)
        append('|').append(category.name)
        append('|').append(name)
        append('|').append(correlationId)
        if (fields.isNotEmpty()) {
            append('|')
            append(fields.entries.joinToString(",") { "${it.key}=${it.value}" })
        }
        throwableType?.let { append("|exception=").append(it) }
        if (appStackFrames.isNotEmpty()) {
            append("|frames=").append(appStackFrames.joinToString(";"))
        }
    }
}
