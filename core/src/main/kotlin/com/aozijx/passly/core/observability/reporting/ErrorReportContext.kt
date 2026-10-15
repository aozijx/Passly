package com.aozijx.passly.core.observability.reporting

import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.OperationCode

data class ErrorReportContext(
    val operation: OperationCode,
    val category: LogCategory,
)
