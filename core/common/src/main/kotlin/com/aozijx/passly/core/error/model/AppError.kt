package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

enum class ErrorLayer { DATA, DOMAIN, UI }

enum class ErrorSeverity { WARNING, ERROR }

@JvmInline
value class AppErrorCode(
    val value: String,
) {
    init {
        require(value.matches(Regex("^[A-Z][A-Z0-9_]{2,63}$"))) { "Invalid AppError code" }
    }
}

sealed class AppError(
    open val code: AppErrorCode,
    open val layer: ErrorLayer,
    open val recoverable: Boolean,
    open val severity: ErrorSeverity,
    open val errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    open val throwableType: String? = null,
    open val reasonCode: String? = null,
) : Exception(code.value, null) {

    companion object
}

sealed class DataError(
    code: AppErrorCode,
    recoverable: Boolean,
    severity: ErrorSeverity,
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
    reasonCode: String? = null,
) : AppError(code, ErrorLayer.DATA, recoverable, severity, errorId, throwableType, reasonCode)

sealed class DomainError(
    code: AppErrorCode,
    recoverable: Boolean,
    severity: ErrorSeverity,
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
    reasonCode: String? = null,
) : AppError(code, ErrorLayer.DOMAIN, recoverable, severity, errorId, throwableType, reasonCode)

class Unexpected(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
        code = CatalogErrorCodes.Application.Runtime.UNEXPECTED,
        recoverable = false,
        severity = ErrorSeverity.ERROR,
        errorId = errorId,
        throwableType = throwableType,
    )
