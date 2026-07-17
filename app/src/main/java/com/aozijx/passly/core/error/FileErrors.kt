package com.aozijx.passly.core.error

// ===== 文件领域错误 =====

class FileIOError(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = FILE_IO_ERROR,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)
