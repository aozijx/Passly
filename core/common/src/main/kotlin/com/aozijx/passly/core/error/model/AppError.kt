package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

enum class ErrorLayer { DATA, DOMAIN, UI }

enum class ErrorSeverity { WARNING, ERROR }

private val ERROR_CODE = Regex("[A-Z][A-Z0-9_]{2,63}")

sealed class AppError(
    open val code: String,
    open val layer: ErrorLayer,
    open val recoverable: Boolean,
    open val severity: ErrorSeverity,
    open val errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    open val throwableType: String? = null,
) : Exception(code, null) {
    init {
        require(code.matches(ERROR_CODE)) { "Error code must be UPPER_SNAKE: $code" }
    }

    companion object
}

sealed class DataError(
    code: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : AppError(code, ErrorLayer.DATA, recoverable, severity, errorId, throwableType)

sealed class DomainError(
    code: String,
    recoverable: Boolean,
    severity: ErrorSeverity,
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : AppError(code, ErrorLayer.DOMAIN, recoverable, severity, errorId, throwableType)

class Unexpected(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = UNEXPECTED,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)
