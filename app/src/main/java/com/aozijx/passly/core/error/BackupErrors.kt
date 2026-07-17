package com.aozijx.passly.core.error

// ===== 备份领域错误 =====

class BackupFailed(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = BACKUP_FAILED,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)
