package com.aozijx.passly.app.observability

import android.os.Process
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.fields
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface CrashHandlerRegistry {
    fun current(): Thread.UncaughtExceptionHandler?
    fun install(handler: Thread.UncaughtExceptionHandler?)
}

internal fun interface ProcessTerminator {
    fun terminate()
}

@Singleton
class ObservabilityRuntimeController @Inject internal constructor(
    private val store: LogRecordStore,
    private val frameworkLogger: FrameworkLogger,
    private val loggerRuntime: LoggerRuntimeGateway,
    private val previousExitSource: PreviousExitSource,
    private val crashHandlers: CrashHandlerRegistry,
    private val processTerminator: ProcessTerminator,
    private val recordEncoder: LogRecordJsonEncoder = LogRecordJsonEncoder(),
) : DiagnosticsLogStore {
    private val started = AtomicBoolean(false)
    private val shutdown = AtomicBoolean(false)
    private val handlingCrash = AtomicBoolean(false)
    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
        if (!handlingCrash.compareAndSet(false, true)) {
            delegateCrash(thread, throwable)
            return@UncaughtExceptionHandler
        }
        try {
            val record = frameworkLogger.fatal(
                operation = Operations.ApplicationLifecycle,
                event = Events.Application.UnhandledException,
                errorCode = ErrorCodes.ApplicationUnhandledException,
                reason = Reasons.UnknownFailure,
                throwable = throwable,
            )
            val flushed = runCatching { store.flush(CRASH_FLUSH_TIMEOUT_MS) }.getOrDefault(false)
            if (!flushed && record != null) {
                runCatching { store.crashEmergencyWrite(record, CRASH_EMERGENCY_TIMEOUT_MS) }
            }
        } catch (_: Throwable) {
            // Crash diagnostics must never replace the original failure.
        } finally {
            delegateCrash(thread, throwable)
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        loggerRuntime.install(frameworkLogger)
        previousCrashHandler = crashHandlers.current()
        crashHandlers.install(crashHandler)
        previousExitSource.readUnrecorded()?.let(::recordPreviousExit)
    }

    fun flush(timeoutMs: Long = LIFECYCLE_FLUSH_TIMEOUT_MS): Boolean =
        runCatching { store.flush(timeoutMs) }.getOrDefault(false)

    override suspend fun readLines(limit: Int): List<String> = withContext(Dispatchers.IO) {
        require(limit > 0) { "Line limit must be positive" }
        readRecords(limit).map(recordEncoder::encode)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) { store.clear() }

    fun shutdown() {
        if (!shutdown.compareAndSet(false, true)) return
        if (crashHandlers.current() === crashHandler) crashHandlers.install(previousCrashHandler)
        loggerRuntime.uninstall(frameworkLogger)
        runCatching { store.close() }
    }

    internal fun readRecords(limit: Int): List<com.aozijx.passly.core.observability.SanitizedLogRecord> {
        val result = ArrayList<com.aozijx.passly.core.observability.SanitizedLogRecord>(limit)
        var cursor: com.aozijx.passly.core.observability.sink.LogRecordCursor? = null
        do {
            val page = store.readPage(cursor, minOf(PAGE_SIZE, limit - result.size))
            result += page.records.take(limit - result.size)
            cursor = page.nextCursor
        } while (cursor != null && result.size < limit)
        return result
    }

    private fun recordPreviousExit(exit: PreviousExit) {
        frameworkLogger.info(
            operation = Operations.ApplicationStartup,
            event = Events.Application.PreviousProcessExit,
            fields = fields {
                enum(LogFieldKeys.ExitReason, exit.reason)
                if (exit.status >= 0) count(LogFieldKeys.ExitStatus, exit.status)
                enum(LogFieldKeys.Importance, exit.importance)
                count(LogFieldKeys.PssKb, exit.pssKb)
                count(LogFieldKeys.RssKb, exit.rssKb)
            },
        )
    }

    private fun delegateCrash(thread: Thread, throwable: Throwable) {
        previousCrashHandler?.uncaughtException(thread, throwable) ?: processTerminator.terminate()
    }

    private companion object {
        const val PAGE_SIZE = 500
        const val LIFECYCLE_FLUSH_TIMEOUT_MS = 300L
        const val CRASH_FLUSH_TIMEOUT_MS = 300L
        const val CRASH_EMERGENCY_TIMEOUT_MS = 200L
    }
}

internal object PlatformCrashHandlerRegistry : CrashHandlerRegistry {
    override fun current(): Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun install(handler: Thread.UncaughtExceptionHandler?) {
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }
}

internal object AndroidProcessTerminator : ProcessTerminator {
    override fun terminate() = Process.killProcess(Process.myPid())
}
