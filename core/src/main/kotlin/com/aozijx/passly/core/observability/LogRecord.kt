package com.aozijx.passly.core.observability

import java.util.UUID

enum class LogLevel(val wireId: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4),
    ;

    companion object {
        fun fromWireId(wireId: Int): LogLevel? = entries.firstOrNull { it.wireId == wireId }
    }
}

enum class LogCategory(val wireId: Int) {
    APPLICATION(0),
    DATABASE(1),
    SECURITY(2),
    AUTOFILL(3),
    BACKUP(4),
    VAULT(5),
    ;

    companion object {
        fun fromWireId(wireId: Int): LogCategory? = entries.firstOrNull { it.wireId == wireId }
    }
}

@JvmInline
value class EventId(val value: String) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid event id" }
    }

    companion object {
        fun new(): EventId = EventId(UUID.randomUUID().toString().replace("-", ""))
    }
}

@JvmInline
value class TraceId(val value: String) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid trace id" }
    }
}

@JvmInline
value class SpanId(val value: String) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid span id" }
    }
}

@JvmInline
value class ProcessSessionId(val value: String) {
    init {
        require(value.matches(OPAQUE_ID_PATTERN)) { "Invalid process session id" }
    }
}

@JvmInline
value class SecureSessionId(val value: String) {
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
value class ExceptionType(val value: String) {
    init {
        require(value.matches(Regex("^[A-Za-z][A-Za-z0-9_$]{0,63}$"))) {
            "Invalid exception type"
        }
    }
}

enum class ExceptionKind(val wireId: Int) {
    DATABASE_IO(0),
    CRYPTO(1),
    AUTHENTICATION(2),
    PLATFORM(3),
    LIFECYCLE(4),
    IO(5),
    UNKNOWN(6),
    ;

    companion object {
        fun fromWireId(wireId: Int): ExceptionKind? = entries.firstOrNull { it.wireId == wireId }
    }
}

@JvmInline
value class AppStackFrame(val value: String) {
    init {
        require(value.length <= 256 && value.matches(APP_STACK_FRAME_PATTERN)) {
            "Invalid application stack frame"
        }
    }
}

data class ExceptionSnapshot(
    val type: ExceptionType,
    val appStackFrames: List<AppStackFrame>,
    val causeTypes: List<ExceptionType>,
    val foreignFrameCount: Int,
    val kind: ExceptionKind = ExceptionKind.UNKNOWN,
) {
    init {
        require(appStackFrames.size <= 24) { "Too many application stack frames" }
        require(causeTypes.size <= 3) { "Too many exception cause types" }
        require(foreignFrameCount >= 0) { "Foreign frame count cannot be negative" }
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
internal val APP_STACK_FRAME_PATTERN =
    Regex(
        "^[A-Za-z0-9_$.-]{1,160}#[A-Za-z0-9_$<>-]{1,64}" +
            "\\([A-Za-z][A-Za-z0-9_$-]{0,95}\\.(?:kt|java):[1-9][0-9]{0,6}\\)$",
    )

fun EventKey.category(): LogCategory = when (value.substringBefore('.')) {
    "application" -> LogCategory.APPLICATION
    "database" -> LogCategory.DATABASE
    "security" -> LogCategory.SECURITY
    "autofill" -> LogCategory.AUTOFILL
    "backup" -> LogCategory.BACKUP
    "vault" -> LogCategory.VAULT
    else -> throw IllegalArgumentException("Event category is not registered")
}
