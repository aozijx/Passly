package com.aozijx.passly.core.error

// ===== 业务逻辑领域错误 =====

class ValidationError(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : DomainError(
    code = VALIDATION_ERROR,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class NotFound(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : DomainError(
    code = NOT_FOUND,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class RateLimited(
    message: String = "操作过于频繁，请稍后重试",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : DomainError(
    code = RATE_LIMITED,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)
