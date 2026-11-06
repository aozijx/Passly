package com.aozijx.passly.core.observability

import java.util.UUID

enum class LogLevel(
    val wireId: Int,
    val exportValue: String,
) {
    DEBUG(0, "DEBUG"),
    INFO(1, "INFO"),
    WARN(2, "WARN"),
    ERROR(3, "ERROR"),
    FATAL(4, "FATAL"),
    ;

    companion object {
        fun fromWireId(wireId: Int): LogLevel? = entries.firstOrNull { it.wireId == wireId }
    }
}

enum class LogCategory(
    val wireId: Int,
    val exportValue: String,
) {
    APPLICATION(0, "APPLICATION"),
    DATABASE(1, "DATABASE"),
    SECURITY(2, "SECURITY"),
    AUTOFILL(3, "AUTOFILL"),
    BACKUP(4, "BACKUP"),
    VAULT(5, "VAULT"),
    ;

    companion object {
        fun fromWireId(wireId: Int): LogCategory? = entries.firstOrNull { it.wireId == wireId }
    }
}

@JvmInline
value class EventId(
    val value: String,
) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid event id" }
    }

    companion object {
        fun new(): EventId = EventId(UUID.randomUUID().toString().replace("-", ""))
    }
}

@JvmInline
value class TraceId(
    val value: String,
) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid trace id" }
    }
}

@JvmInline
value class SpanId(
    val value: String,
) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid span id" }
    }
}

@JvmInline
value class ProcessSessionId(
    val value: String,
) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid process session id" }
    }
}

@JvmInline
value class SecureSessionId(
    val value: String,
) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid secure session id" }
    }
}

data class TraceSnapshot(
    val traceId: TraceId,
    val spanId: SpanId,
    val parentSpanId: SpanId?,
)

@JvmInline
value class ExceptionType(
    val value: String,
) {
    init {
        require(value.matches(Regex("^[A-Za-z][A-Za-z0-9_$]{0,63}$"))) {
            "Invalid exception type"
        }
    }
}

enum class ExceptionKind(
    val wireId: Int,
    val exportValue: String,
) {
    DATABASE_IO(0, "DATABASE_IO"),
    CRYPTO(1, "CRYPTO"),
    AUTHENTICATION(2, "AUTHENTICATION"),
    PLATFORM(3, "PLATFORM"),
    LIFECYCLE(4, "LIFECYCLE"),
    IO(5, "IO"),
    UNKNOWN(6, "UNKNOWN"),
    ;

    companion object {
        fun fromWireId(wireId: Int): ExceptionKind? = entries.firstOrNull { it.wireId == wireId }
    }
}

@JvmInline
value class SanitizedStackFrame(
    val value: String,
) {
    init {
        require(value.length <= 256 && value.matches(SANITIZED_STACK_FRAME_PATTERN)) {
            "Invalid sanitized stack frame"
        }
    }
}

data class ExceptionSnapshot(
    val type: ExceptionType,
    val stackFrames: List<SanitizedStackFrame>,
    val causeTypes: List<ExceptionType>,
    val externalFrameCount: Int,
    val kind: ExceptionKind = ExceptionKind.UNKNOWN,
) {
    init {
        require(stackFrames.size <= 24) { "Too many sanitized stack frames" }
        require(causeTypes.size <= 3) { "Too many exception cause types" }
        require(externalFrameCount >= 0) { "External frame count cannot be negative" }
    }
}

data class PendingError(
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable?,
) {
    override fun toString(): String =
        "PendingError(errorCode=$errorCode, reason=$reason, " +
            "throwableType=${throwable?.javaClass?.simpleName})"
}

data class ErrorPayload(
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val exception: ExceptionSnapshot?,
)

data class PendingLogEvent(
    val level: LogLevel,
    val event: EventKey,
    val fields: LogFields = LogFields.Empty,
    val error: PendingError? = null,
)

data class SanitizedLogRecord(
    val eventId: EventId,
    val timestampMs: Long,
    val elapsedRealtimeMs: Long,
    val level: LogLevel,
    val category: LogCategory,
    val event: EventKey,
    val processSessionId: ProcessSessionId,
    val secureSessionId: SecureSessionId?,
    val trace: TraceSnapshot,
    val fields: LogFields,
    val error: ErrorPayload?,
)

internal val OPAQUE_ID_PATTERN = Regex("^[A-Za-z0-9_-]{8,64}$")
internal val SANITIZED_STACK_FRAME_PATTERN =
    Regex(
        "^[A-Za-z0-9_$.-]{1,160}#[A-Za-z0-9_$<>-]{1,64}" +
            "(?:\\([A-Za-z][A-Za-z0-9_$-]{0,95}\\.(?:kt|java)(?::[1-9][0-9]{0,6})?\\))?$",
    )

fun EventKey.category(): LogCategory =
    when (value.substringBefore('.')) {
        "application" -> LogCategory.APPLICATION
        "database" -> LogCategory.DATABASE
        "security" -> LogCategory.SECURITY
        "autofill" -> LogCategory.AUTOFILL
        "backup" -> LogCategory.BACKUP
        "vault" -> LogCategory.VAULT
        else -> throw IllegalArgumentException("Event category is not registered")
    }
