package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.FatalLogStatement
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogStatement
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogSink
import kotlinx.coroutines.runBlocking

/** Typed bridge reserved for Android framework callbacks that cannot receive injected dependencies. */
interface FrameworkLogger {
    fun log(operation: OperationCode, statement: LogStatement)

    fun fatal(
        operation: OperationCode,
        statement: FatalLogStatement,
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

    override fun log(operation: OperationCode, statement: LogStatement) =
        delegate.log(operation, statement)

    override fun fatal(
        operation: OperationCode,
        statement: FatalLogStatement,
    ): SanitizedLogRecord? =
        delegate.fatal(operation, statement)
}

internal class RootTraceFrameworkLogger(
    private val logger: Logger,
    private val crashCapture: CrashRecordCaptureSink,
) : FrameworkLogger {
    override fun log(operation: OperationCode, statement: LogStatement) {
        safelyRun {
            logger.withRootTrace(operation) { logger.log(statement) }
        }
    }

    override fun fatal(
        operation: OperationCode,
        statement: FatalLogStatement,
    ): SanitizedLogRecord? {
        crashCapture.clear()
        safelyRun {
            logger.withRootTrace(operation) { logger.log(statement) }
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
    override fun log(operation: OperationCode, statement: LogStatement) = Unit

    override fun fatal(
        operation: OperationCode,
        statement: FatalLogStatement,
    ): SanitizedLogRecord? = null
}
