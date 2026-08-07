package com.aozijx.passly.core.error

// ===== 加密/安全领域错误 =====

class CryptoError(
    message: String,
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = CRYPTO_ERROR,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)

class KeyStateError(
    message: String = "密钥状态异常",
    val detail: String = "",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = KEY_STATE_ERROR,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)

class CryptoDataCorrupted(
    message: String = "加密数据损坏",
    val detail: String = "",
    trace: ErrorTrace = ErrorTrace(ErrorLayer.DATA),
    cause: Throwable? = null
) : DataError(
    code = CRYPTO_DATA_CORRUPTED,
    message = message,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    trace = trace,
    cause = cause
)
