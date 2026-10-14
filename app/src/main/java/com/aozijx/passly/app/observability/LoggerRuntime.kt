package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogSink
import kotlinx.coroutines.runBlocking

/** Typed bridge reserved for Android framework callbacks that cannot receive injected dependencies. */
interface FrameworkLogger {
    fun debug(operation: OperationCode, event: EventKey, fields: LogFields = LogFields.Empty)
    fun info(operation: OperationCode, event: EventKey, fields: LogFields = LogFields.Empty)
    fun error(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId?

    fun fatal(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): SanitizedLogRecord?
}

interface LoggerRuntimeGateway {
    fun install(logger: FrameworkLogger)
    fun uninstall(logger: FrameworkLogger)
}

object LoggerRuntime : LoggerRuntimeGateway, FrameworkLogger {
    @Volatile
    private var delegate: FrameworkLogger = NoOpFrameworkLogger

    @Synchronized
    override fun install(logger: FrameworkLogger) {
        delegate = logger
    }

    @Synchronized
    override fun uninstall(logger: FrameworkLogger) {
        if (delegate === logger) delegate = NoOpFrameworkLogger
    }

    override fun debug(operation: OperationCode, event: EventKey, fields: LogFields) =
        delegate.debug(operation, event, fields)

    override fun info(operation: OperationCode, event: EventKey, fields: LogFields) =
        delegate.info(operation, event, fields)

    override fun error(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId? = delegate.error(operation, event, errorCode, reason, throwable, fields)

    override fun fatal(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): SanitizedLogRecord? =
        delegate.fatal(operation, event, errorCode, reason, throwable, fields)
}

internal class RootTraceFrameworkLogger(
    private val logger: Logger,
    private val crashCapture: CrashRecordCaptureSink,
) : FrameworkLogger {
    override fun debug(operation: OperationCode, event: EventKey, fields: LogFields) {
        safelyRun { logger.withRootTrace(operation) { logger.debug(event, fields) } }
    }

    override fun info(operation: OperationCode, event: EventKey, fields: LogFields) {
        safelyRun { logger.withRootTrace(operation) { logger.info(event, fields) } }
    }

    override fun error(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId? = safelyRun {
        logger.withRootTrace(operation) { logger.error(event, errorCode, reason, throwable, fields) }
    }

    override fun fatal(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): SanitizedLogRecord? {
        crashCapture.clear()
        safelyRun {
            logger.withRootTrace(operation) { logger.fatal(event, errorCode, reason, throwable, fields) }
        }
        return crashCapture.take()
    }

    private fun <T> safelyRun(block: suspend () -> T): T? = try {
        runBlocking { block() }
    } catch (_: Exception) {
        null
    }
}

internal class CrashRecordCaptureSink : LogSink {
    private val captured = ThreadLocal<SanitizedLogRecord?>()

    override fun emit(record: SanitizedLogRecord) {
        if (record.level == LogLevel.FATAL) captured.set(record)
    }

    fun clear() = captured.remove()
    fun take(): SanitizedLogRecord? = captured.get().also { captured.remove() }
}

private object NoOpFrameworkLogger : FrameworkLogger {
    override fun debug(operation: OperationCode, event: EventKey, fields: LogFields) = Unit
    override fun info(operation: OperationCode, event: EventKey, fields: LogFields) = Unit
    override fun error(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId? = null

    override fun fatal(
        operation: OperationCode,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): SanitizedLogRecord? = null
}
