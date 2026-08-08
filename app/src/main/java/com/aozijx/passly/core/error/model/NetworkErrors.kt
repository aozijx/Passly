package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 网络领域错误 =====

class NetworkError(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = NETWORK_ERROR,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)