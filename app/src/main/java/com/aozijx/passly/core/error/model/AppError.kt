package com.aozijx.passly.core.error

import com.github.f4b6a3.uuid.UuidCreator

enum class ErrorLayer { DATA, DOMAIN, UI }

enum class ErrorSeverity { WARNING, ERROR }

sealed interface ErrorTraceValue {
    data class Count(val value: Long) : ErrorTraceValue {
        init {
            require(value >= 0) { "Count must not be negative" }
        }
    }

    data class Flag(val value: Boolean) : ErrorTraceValue

    data class Code(val value: String) : ErrorTraceValue {
        init {
            require(value.matches(SAFE_CODE)) { "Invalid trace code" }
        }
    }

    private companion object {
        val SAFE_CODE = Regex("[a-z][a-z0-9_.-]{0,95}")
    }
}

data class ErrorTrace(
    val originLayer: ErrorLayer,
    val operation: String? = null,
    val traceId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val context: Map<String, ErrorTraceValue> = emptyMap()
) {
    fun withContext(vararg pairs: Pair<String, ErrorTraceValue>) =
        copy(context = context + pairs.toMap())
}

private val ERROR_CODE = Regex("[A-Z][A-Z0-9_]{2,63}")

sealed class AppError(
    open val code: String,
    override val message: String,
    open val layer: ErrorLayer,
    open val recoverable: Boolean,
    open val severity: ErrorSeverity,
    open val trace: ErrorTrace,
    override val cause: Throwable?
) : Exception(message, cause) {
    init {
        require(code.matches(ERROR_CODE)) { "Error code must be UPPER_SNAKE: $code" }
        require(message.isNotBlank()) { "AppError message must not be blank" }
    }

    companion object
}

sealed class DataError(
    code: String,
    message: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    trace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
    cause: Throwable? = null
) : AppError(code, message, ErrorLayer.DATA, recoverable, severity, trace, cause)

sealed class DomainError(
    code: String,
    message: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    trace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : AppError(code, message, ErrorLayer.DOMAIN, recoverable, severity, trace, cause)

class Unexpected(
    message: String = "发生未知错误",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = UNEXPECTED,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)
