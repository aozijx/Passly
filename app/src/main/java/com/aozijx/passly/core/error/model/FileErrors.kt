package com.aozijx.passly.core.error.model

import com.github.f4b6a3.uuid.UuidCreator

// ===== 文件领域错误 =====

class FileIOError(
    errorId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    throwableType: String? = null,
) : DataError(
    code = FILE_IO_ERROR,
    recoverable = false,
    severity = ErrorSeverity.ERROR,
    errorId = errorId,
    throwableType = throwableType,
)