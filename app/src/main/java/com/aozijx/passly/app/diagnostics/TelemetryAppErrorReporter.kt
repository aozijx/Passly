package com.aozijx.passly.app.diagnostics

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.ErrorSeverity
import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEmitter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.reporting.AppErrorReporter
import com.aozijx.passly.core.telemetry.reporting.ErrorReportContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AppErrorReporter] 的遥测实现。
 *
 * 只能写入白名单字段，禁止写入 error.message、Throwable.message、entryId、路径等。
 */
@Singleton
class TelemetryAppErrorReporter @Inject constructor(
    private val emitter: TelemetryEmitter
) : AppErrorReporter {

    override fun report(error: AppError, context: ErrorReportContext) {
        val level = error.severity.toEventLevel()
        val event = TelemetryEvent(
            level = level,
            category = context.category,
            name = "error.${error.code.lowercase()}",
            fields = buildFields(error, context),
            throwableType = error.throwableType
                ?.takeIf { SAFE_TYPE_NAME.matches(it) },
            correlationId = error.errorId
        )
        emitter.emit(event)
    }

    private fun buildFields(
        error: AppError,
        context: ErrorReportContext
    ): Map<String, SafeLogValue> = buildMap {
        put("error_code", SafeLogValue.ErrorCodeValue(ErrorCode(error.code)))
        put("error_layer", SafeLogValue.EnumName(error.layer.name))
        put("severity", SafeLogValue.EnumName(error.severity.name))
        put("recoverable", SafeLogValue.BooleanValue(error.recoverable))
        put("operation", SafeLogValue.OperationCodeValue(context.operation))
    }

    private companion object {
        private val SAFE_TYPE_NAME = Regex("[A-Z][A-Za-z0-9_]{0,63}")
    }
}

private fun ErrorSeverity.toEventLevel(): EventLevel = when (this) {
    ErrorSeverity.WARNING -> EventLevel.WARN
    ErrorSeverity.ERROR -> EventLevel.ERROR
}