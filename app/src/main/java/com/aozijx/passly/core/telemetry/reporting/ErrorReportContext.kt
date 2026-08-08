package com.aozijx.passly.core.telemetry.reporting

import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.OperationCode

/**
 * 错误报告上下文。
 *
 * 只包含遥测所需的安全字段，不包含 error.message、entryId、路径等。
 */
data class ErrorReportContext(
    val operation: OperationCode,
    val category: EventCategory
)