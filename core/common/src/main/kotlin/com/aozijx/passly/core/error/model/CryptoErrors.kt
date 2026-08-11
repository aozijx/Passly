package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 加密/安全领域错误 =====

class CryptoError(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = CRYPTO_ERROR,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)

class CryptoDataCorrupted(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = CRYPTO_DATA_CORRUPTED,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)
