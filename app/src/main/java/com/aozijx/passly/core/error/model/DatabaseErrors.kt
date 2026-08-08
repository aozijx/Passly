package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 数据库领域错误 =====

class DatabaseLocked(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = DATABASE_LOCKED,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)

class DatabaseInitFailed(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = DATABASE_INIT_FAILED,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)