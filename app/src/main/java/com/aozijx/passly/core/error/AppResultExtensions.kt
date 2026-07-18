package com.aozijx.passly.core.error

import com.aozijx.passly.core.diagnostics.AppLog

// 自动根据错误严重级别记录日志
/**
 * 自动日志记录：如果失败，根据错误级别写入结构化诊断系统。
 */
fun <T> AppResult<T>.onFailureLog(tag: String = "DataLayer"): AppResult<T> {
    return this.onFailure { error ->
        val logMsg = "[${error.code}] ${error.trace.operation ?: "UnknownOp"}: ${error.message}"
        when (error.severity) {
            ErrorSeverity.ERROR -> AppLog.e(tag, logMsg, error)
            ErrorSeverity.WARNING -> AppLog.w(tag, logMsg, error)
        }
    }
}

// 带上下文信息的日志记录（例如：操作名称）
fun <T> AppResult<T>.logFailureWithContext(
    tag: String = "AppResult",
    operation: String,
    context: Map<String, String> = emptyMap()
): AppResult<T> {
    if (this is AppResult.Failure) {
        val trace = error.trace
        val contextStr = if (context.isNotEmpty()) " | context=$context" else ""
        val msg =
            "[$operation] ${error.code} - ${error.message} | traceId=${trace.traceId}$contextStr"
        when (error.severity) {
            ErrorSeverity.WARNING -> AppLog.w(tag, msg, error)
            ErrorSeverity.ERROR -> AppLog.e(tag, msg, error)
        }
    }
    return this
}

/**
 * 当结果为 Failure 时，将错误转换为新错误并返回新的 Failure。
 * 适用于需要将底层错误映射为 UI 层安全的错误（如 NetworkError → Unexpected）。
 */
inline fun <T> AppResult<T>.onFailureMap(
    transform: (AppError) -> AppError
): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.failure(transform(error))
}
