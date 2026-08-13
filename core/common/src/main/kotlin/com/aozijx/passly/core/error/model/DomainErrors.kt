package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 业务逻辑领域错误 =====

class ValidationError(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DomainError(
    code = VALIDATION_ERROR,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)

class NotFound(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DomainError(
    code = NOT_FOUND,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)

class Conflict(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DomainError(
    code = CONFLICT,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)

class SessionModeRestricted(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DomainError(
    code = SESSION_MODE_RESTRICTED,
    recoverable = true,
    severity = ErrorSeverity.WARNING,
    errorId = errorId,
    throwableType = throwableType,
)
