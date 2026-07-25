package com.aozijx.passly.app.telemetry

import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.SafeLogValue.Count
import com.aozijx.passly.core.telemetry.SafeLogValue.EnumName
import com.aozijx.passly.core.telemetry.SafeLogValue.ErrorCodeValue
import com.aozijx.passly.core.telemetry.SafeLogValue.OperationCodeValue
import com.aozijx.passly.core.telemetry.TelemetryEmitter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity

/**
 * Failure → Telemetry 安全映射扩展。
 *
 * 只能写入：
 * - failure code
 * - origin
 * - severity
 * - operation code
 * - correlation ID
 *
 * 不能写 failure.message、Throwable message、路径、URL、用户名等。
 *
 * 使用方式：
 * ```kotlin
 * telemetry.recordFailure(failure, OperationCode("auth_verify"))
 * ```
 */
fun TelemetryEmitter.recordFailure(
    failure: AppFailure,
    operation: OperationCode
) {
    val level = failure.severity.toEventLevel()
    val category = failure.origin.toEventCategory()
    val event = TelemetryEvent(
        level = level,
        category = category,
        name = "failure.${failure.code.lowercase()}",
        fields = mapOf(
            "code" to ErrorCodeValue(ErrorCode(failure.code)),
            "origin" to EnumName(failure.origin.name),
            "severity" to EnumName(failure.severity.name),
            "operation" to OperationCodeValue(operation),
            "recovery" to EnumName(failure.recoveryAction.name)
        ),
        correlationId = failure.correlationId
    )
    emit(event)
}

/**
 * 记录失败计数（不含任何细节）。
 */
fun TelemetryEmitter.recordFailureCount(
    operation: OperationCode
) {
    val event = TelemetryEvent(
        level = EventLevel.INFO,
        category = EventCategory.SECURITY,
        name = "failure.count.${operation.value}",
        fields = mapOf("count" to Count(1L))
    )
    emit(event)
}

private fun FailureSeverity.toEventLevel(): EventLevel = when (this) {
    FailureSeverity.INFO -> EventLevel.INFO
    FailureSeverity.WARNING -> EventLevel.WARN
    FailureSeverity.ERROR -> EventLevel.ERROR
    FailureSeverity.FATAL -> EventLevel.FATAL
}

private fun FailureOrigin.toEventCategory(): EventCategory = when (this) {
    FailureOrigin.SECURITY -> EventCategory.SECURITY
    FailureOrigin.DATA -> EventCategory.DATABASE
    FailureOrigin.PLATFORM -> EventCategory.APPLICATION
    FailureOrigin.DOMAIN -> EventCategory.APPLICATION
    FailureOrigin.PRESENTATION -> EventCategory.UI
}
