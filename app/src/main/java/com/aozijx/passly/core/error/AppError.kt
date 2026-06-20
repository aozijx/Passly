package com.aozijx.passly.core.error

import java.util.UUID

enum class ErrorLayer {
    DATA,
    DOMAIN,
    UI
}

data class ErrorTrace(
    val traceId: String = UUID.randomUUID().toString(),
    val originLayer: ErrorLayer,
    val operation: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val extras: Map<String, String> = emptyMap()
)

sealed class AppError(
    val code: String,
    override val message: String,
    val layer: ErrorLayer,
    val recoverable: Boolean = false,
    val trace: ErrorTrace,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class AuthFailed(
        override val message: String,
        val errorTrace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
        override val cause: Throwable? = null
    ) : AppError(
        code = "AUTH_FAILED",
        message = message,
        layer = ErrorLayer.DATA,
        recoverable = true,
        trace = errorTrace,
        cause = cause
    )

    class DatabaseLocked(
        errorTrace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA)
    ) : AppError(
        code = "DATABASE_LOCKED",
        message = "数据库已锁定，请先解锁",
        layer = ErrorLayer.DATA,
        recoverable = true,
        trace = errorTrace
    )

    data class DatabaseInitFailed(
        override val message: String = "数据库初始化失败",
        val errorTrace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
        override val cause: Throwable? = null
    ) : AppError(
        code = "DATABASE_INIT_FAILED",
        message = message,
        layer = ErrorLayer.DATA,
        recoverable = false,
        trace = errorTrace,
        cause = cause
    )

    data class BackupFailed(
        override val message: String,
        val errorTrace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
        override val cause: Throwable? = null
    ) : AppError(
        code = "BACKUP_FAILED",
        message = message,
        layer = ErrorLayer.DATA,
        recoverable = true,
        trace = errorTrace,
        cause = cause
    )

    data class Unexpected(
        override val message: String = "发生未知错误",
        val errorTrace: ErrorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
        override val cause: Throwable? = null
    ) : AppError(
        code = "UNEXPECTED",
        message = message,
        layer = ErrorLayer.DATA,
        recoverable = false,
        trace = errorTrace,
        cause = cause
    )

    companion object {
        fun fromThrowable(
            throwable: Throwable,
            layer: ErrorLayer,
            operation: String? = null
        ): AppError {
            return when (throwable) {
                is AppError -> throwable
                else -> Unexpected(
                    message = throwable.message ?: "发生未知错误",
                    errorTrace = ErrorTrace(originLayer = layer, operation = operation),
                    cause = throwable
                )
            }
        }
    }
}