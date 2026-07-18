package com.aozijx.passly.core.error

import com.github.f4b6a3.uuid.UuidCreator

// ─── 基础定义 ──────────────────────────────
enum class ErrorLayer { DATA, DOMAIN, UI }

enum class ErrorSeverity { WARNING, ERROR }

data class ErrorTrace(
    val originLayer: ErrorLayer,
    val operation: String? = null,
    val traceId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val extras: Map<String, String> = emptyMap()
) {
    fun withExtras(vararg pairs: Pair<String, String>) = copy(extras = extras + pairs.toMap())
}

// ─── 基类 ──────────────────────────────
sealed class AppError(
    open val code: String,
    override val message: String,
    open val layer: ErrorLayer,
    open val recoverable: Boolean,
    open val severity: ErrorSeverity,
    open val trace: ErrorTrace,
    override val cause: Throwable?
) : Exception(message, cause) {
    companion object
}

// 数据层错误基类
sealed class DataError(
    code: String,
    message: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    trace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA), // 默认值
    cause: Throwable? = null
) : AppError(code, message, ErrorLayer.DATA, recoverable, severity, trace, cause)

// 业务层错误基类
sealed class DomainError(
    code: String,
    message: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    trace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : AppError(code, message, ErrorLayer.DOMAIN, recoverable, severity, trace, cause)

// ─── 兜底错误 ──────────────────────────────
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
