package com.aozijx.passly.core.error

// ===== 数据库领域错误 =====

class DatabaseLocked(
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA)
) : DataError(
    code = DATABASE_LOCKED,
    message = "数据库已锁定，请先解锁",
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace
)

class DatabaseInitFailed(
    message: String = "数据库初始化失败",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = DATABASE_INIT_FAILED,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)
