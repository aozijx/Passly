package com.aozijx.passly.core.observability

import java.util.UUID

enum class LogLevel { DEBUG, INFO, WARN, ERROR, FATAL }

enum class LogCategory { APPLICATION, DATABASE, SECURITY, AUTOFILL, BACKUP }

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
value class DiagnosticId(val value: String) {
    init {
        require(value.matches(Regex("^DGN-[A-Z2-9]{4}-[A-Z2-9]{4}$"))) { "Invalid diagnostic id" }
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun fromEntropy(entropy: ByteArray): DiagnosticId {
            require(entropy.size >= 7) { "Diagnostic id requires at least 7 bytes" }
            val body = CharArray(7) { index -> ALPHABET[entropy[index].toInt() and 31] }
            val checksum = ALPHABET[body.fold(0) { value, char -> value + char.code } and 31]
            val encoded = String(body) + checksum
            return DiagnosticId("DGN-${encoded.take(4)}-${encoded.drop(4)}")
        }
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

@JvmInline
value class AppStackFrame internal constructor(val value: String) {
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
    val diagnosticId: DiagnosticId,
) {
    override fun toString(): String =
        "PendingError(errorCode=$errorCode, reason=$reason, " +
            "throwableType=${throwable?.javaClass?.simpleName}, diagnosticId=$diagnosticId)"
}

data class ErrorPayload(
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val diagnosticId: DiagnosticId,
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
    Regex("^[A-Za-z0-9_$.-]{1,160}#[A-Za-z0-9_$<>-]{1,64}:[1-9][0-9]{0,6}$")

fun EventKey.category(): LogCategory = when (value.substringBefore('.')) {
    "application" -> LogCategory.APPLICATION
    "database" -> LogCategory.DATABASE
    "security" -> LogCategory.SECURITY
    "autofill" -> LogCategory.AUTOFILL
    "backup" -> LogCategory.BACKUP
    else -> throw IllegalArgumentException("Event category is not registered")
}
