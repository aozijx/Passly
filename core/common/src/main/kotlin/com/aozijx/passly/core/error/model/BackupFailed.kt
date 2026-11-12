package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

class BackupFailed(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
    val reasonCode: String? = null,
) : DataError(
        code = CatalogErrorCodes.BACKUP_FAILED,
        recoverable = true,
        severity = ErrorSeverity.ERROR,
        errorId = errorId,
        throwableType = throwableType,
    )
