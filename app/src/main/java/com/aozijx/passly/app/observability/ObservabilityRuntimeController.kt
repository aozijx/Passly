package com.aozijx.passly.app.observability

import android.os.Process
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

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
    private val logger: Logger,
    private val crashCapture: CrashRecordCaptureSink,
    private val previousExitSource: PreviousExitSource,
    private val crashHandlers: CrashHandlerRegistry,
    private val processTerminator: ProcessTerminator,
    private val eventAggregator: EventAggregator,
    private val recordEncoder: LogRecordTextEncoder = LogRecordTextEncoder(),
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
            val record = captureFatal(throwable)
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
        previousCrashHandler = crashHandlers.current()
        crashHandlers.install(crashHandler)
        previousExitSource.readUnrecorded()?.let(::recordPreviousExit)
    }

    fun flush(timeoutMs: Long = LIFECYCLE_FLUSH_TIMEOUT_MS): Boolean {
        eventAggregator.flush()
        return runCatching { store.flush(timeoutMs) }.getOrDefault(false)
    }

    override suspend fun readLines(limit: Int): List<String> = withContext(Dispatchers.IO) {
        require(limit > 0) { "Line limit must be positive" }
        readRecords(limit).map(recordEncoder::encode)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) { store.clear() }

    fun shutdown() {
        if (!shutdown.compareAndSet(false, true)) return
        eventAggregator.flush()
        if (crashHandlers.current() === crashHandler) crashHandlers.install(previousCrashHandler)
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
        runCatching {
            runBlocking {
                logger.withRootTrace(Operations.ApplicationStartup) {
                    logger.log(
                        LogStatements.Application.previousProcessExit(
                            exitReason = exit.reason,
                            status = exit.status,
                            importance = exit.importance,
                            pssKb = exit.pssKb,
                            rssKb = exit.rssKb,
                        ),
                    )
                }
            }
        }
    }

    private fun captureFatal(throwable: Throwable): com.aozijx.passly.core.observability.SanitizedLogRecord? {
        crashCapture.clear()
        runCatching {
            runBlocking {
                logger.withRootTrace(Operations.ApplicationLifecycle) {
                    logger.log(LogStatements.Application.unhandledException(throwable))
                }
            }
        }
        return crashCapture.take()
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
