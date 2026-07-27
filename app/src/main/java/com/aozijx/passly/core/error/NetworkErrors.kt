package com.aozijx.passly.core.error

// ===== 网络领域错误 =====

class NetworkError(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = NETWORK_ERROR,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)
