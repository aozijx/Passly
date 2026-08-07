package com.aozijx.passly.core.error

import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.SafeLogValue

/**
 * 如果结果失败，根据错误级别写入诊断系统。
 *
 * 这是 opt-in 机制：在 repository / use case 边界调用，避免每一层重复记录同一个错误。
 */
fun <T> AppResult<T>.onFailureLog(tag: String = "DataLayer"): AppResult<T> {
    return onFailure { error ->
        AppTelemetry.logAppError(error, tag, emptyMap())
    }
}

/**
 * 带额外上下文的失败日志。
 */
fun <T> AppResult<T>.logFailureWithContext(
    tag: String = "AppResult",
    operation: String,
    context: Map<String, ErrorTraceValue> = emptyMap()
): AppResult<T> {
    if (this is AppResult.Failure) {
        val operationContext =
            context + ("operation" to ErrorTraceValue.Code(operation.toTraceCode()))
        AppTelemetry.logAppError(error, tag, operationContext)
    }
    return this
}

/**
 * 兼容旧调用名；新代码优先直接使用 [AppResult.mapFailure]。
 */
fun <T> AppResult<T>.onFailureMap(
    transform: (AppError) -> AppError
): AppResult<T> = mapFailure(transform)

private fun AppTelemetry.logAppError(
    error: AppError,
    tag: String,
    context: Map<String, ErrorTraceValue>
) {
    val fields = buildMap {
        put("component", SafeLogValue.EnumName(tag.toEnumName()))
        put("error_code", SafeLogValue.ErrorCodeValue(ErrorCode(error.code)))
        put("origin_layer", SafeLogValue.EnumName(error.trace.originLayer.name))
        put("error_layer", SafeLogValue.EnumName(error.layer.name))
        put("severity", SafeLogValue.EnumName(error.severity.name))
        put("recoverable", SafeLogValue.BooleanValue(error.recoverable))
        put("context_count", SafeLogValue.Count(context.size.toLong()))
        error.trace.operation?.let {
            put("operation", SafeLogValue.OperationCodeValue(OperationCode(it.toOperationCode())))
        }
        context.forEach { (key, value) ->
            val fieldKey = "ctx_${key.toFieldName()}"
            put(fieldKey, value.toSafeLogValue())
        }
    }
    when (error.severity) {
        ErrorSeverity.WARNING -> w(error.telemetryCategory(), "app_error.failure", fields, error)
        ErrorSeverity.ERROR -> e(error.telemetryCategory(), "app_error.failure", fields, error)
    }
}

private fun AppError.telemetryCategory(): EventCategory = when {
    code.startsWith("AUTH_") ||
            code.startsWith("BIOMETRIC_") ||
            code.startsWith("APP_PASSWORD_") ||
            code == APP_LOCKED -> EventCategory.AUTHENTICATION

    code.startsWith("DATABASE_") -> EventCategory.DATABASE
    code.startsWith("BACKUP_") -> EventCategory.BACKUP
    code.startsWith("CRYPTO_") || code.startsWith("KEY_") -> EventCategory.SECURITY
    code.startsWith("NETWORK_") -> EventCategory.NETWORK
    code.startsWith("FILE_") -> EventCategory.FILE_IO
    layer == ErrorLayer.UI -> EventCategory.UI
    else -> EventCategory.APPLICATION
}

private fun ErrorTraceValue.toSafeLogValue(): SafeLogValue = when (this) {
    is ErrorTraceValue.Count -> SafeLogValue.Count(value)
    is ErrorTraceValue.Flag -> SafeLogValue.BooleanValue(value)
    is ErrorTraceValue.Code -> SafeLogValue.OperationCodeValue(OperationCode(value.toOperationCode()))
}

private fun String.toTraceCode(): String =
    lowercase()
        .map { if (it.isLetterOrDigit()) it else '_' }
        .joinToString("")
        .trim('_')
        .take(95)
        .ifBlank { "unknown" }

private fun String.toOperationCode(): String =
    toTraceCode().take(128).let { if (it.length >= 3) it else "unknown" }

private fun String.toEnumName(): String =
    uppercase()
        .map { if (it.isLetterOrDigit()) it else '_' }
        .joinToString("")
        .trim('_')
        .take(64)
        .ifBlank { "UNKNOWN" }

private fun String.toFieldName(): String =
    lowercase()
        .map { if (it.isLetterOrDigit()) it else '_' }
        .joinToString("")
        .trim('_')
        .take(60)
        .ifBlank { "unknown" }
