package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 备份领域错误 =====

class BackupFailed(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = BACKUP_FAILED,
    recoverable = true,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)
