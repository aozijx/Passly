package com.aozijx.passly.core.error

// ===== 认证领域错误 =====

class AuthFailed(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = AUTH_FAILED,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class BiometricUnavailable(
    message: String = "设备不支持生物识别",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = BIOMETRIC_UNAVAILABLE,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class BiometricNotEnrolled(
    message: String = "未录入生物识别信息",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = BIOMETRIC_NOT_ENROLLED,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class BiometricLockedOut(
    message: String = "生物识别已锁定",
    val lockoutDurationMs: Long = 30_000,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = BIOMETRIC_LOCKED_OUT,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class AppPasswordIncorrect(
    message: String = "密码不正确",
    val attemptCount: Int = 0,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = APP_PASSWORD_INCORRECT,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)

class AppLocked(
    message: String = "请先解锁应用",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DOMAIN),
    cause: Throwable? = null
) : DomainError(
    code = APP_LOCKED,
    message = message,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    trace = trace,
    cause = cause
)
