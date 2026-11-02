package com.aozijx.passly.app.observability

import android.os.Process
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal interface CrashHandlerRegistry {
    fun current(): Thread.UncaughtExceptionHandler?

    fun install(handler: Thread.UncaughtExceptionHandler?)
}

internal fun interface ProcessTerminator {
    fun terminate()
}

@Singleton
class ObservabilityRuntimeController
    @Inject
    internal constructor(
        private val store: LogRecordStore,
        private val logger: Logger,
        private val crashCapture: CrashRecordCaptureSink,
        private val previousExitSource: PreviousExitSource,
        private val crashHandlers: CrashHandlerRegistry,
        private val processTerminator: ProcessTerminator,
        private val eventAggregator: EventAggregator,
        private val diagnosticsLogSettings: DiagnosticsLogSettingsRepository,
        private val recordingPolicy: DiagnosticsLogRecordingPolicy,
        private val recordEncoder: TsvRecordEncoder = TsvRecordEncoder(),
    ) : DiagnosticsLogStore {
        private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val started = AtomicBoolean(false)
        private val shutdown = AtomicBoolean(false)
        private val handlingCrash = AtomicBoolean(false)
        private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null
        private var settingsJob: Job? = null

        private val crashHandler =
            Thread.UncaughtExceptionHandler { thread, throwable ->
                if (!handlingCrash.compareAndSet(false, true)) {
                    delegateCrash(thread, throwable)
                    return@UncaughtExceptionHandler
                }
                try {
                    val record = captureFatal(throwable)
                    val flushed =
                        try {
                            store.flush(CRASH_FLUSH_TIMEOUT_MS)
                        } catch (diagnosticFailure: Throwable) {
                            addDiagnosticFailure(throwable, diagnosticFailure)
                            false
                        }
                    if (!flushed && record != null) {
                        try {
                            store.crashEmergencyWrite(record, CRASH_EMERGENCY_TIMEOUT_MS)
                        } catch (diagnosticFailure: Throwable) {
                            addDiagnosticFailure(throwable, diagnosticFailure)
                        }
                    }
                } catch (diagnosticFailure: Throwable) {
                    // Crash diagnostics must never replace the original failure.
                    addDiagnosticFailure(throwable, diagnosticFailure)
                } finally {
                    delegateCrash(thread, throwable)
                }
            }

        fun start() {
            if (!started.compareAndSet(false, true)) return
            previousCrashHandler = crashHandlers.current()
            crashHandlers.install(crashHandler)
            settingsJob =
                settingsScope.launch {
                    var initialModeLoaded = false
                    diagnosticsLogSettings.diagnosticsLogRecordingMode.collect { mode ->
                        recordingPolicy.update(mode)
                        if (!initialModeLoaded) {
                            initialModeLoaded = true
                            previousExitSource.readUnrecorded()?.let(::recordPreviousExit)
                        }
                    }
                }
        }

        fun flush(timeoutMs: Long = LIFECYCLE_FLUSH_TIMEOUT_MS): Boolean {
            eventAggregator.flush()
            val startedAt = System.nanoTime()
            if (!flushStore(timeoutMs)) return false
            val dropped = store.drainDroppedCounts()
            if (dropped.total == 0L) return true
            val summaryRecorded =
                try {
                    runBlocking {
                        logger.withRootTrace(Operations.ApplicationLifecycle) {
                            logger.log(LogStatements.Application.logOverflow(dropped))
                        }
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            if (!summaryRecorded) return false
            val elapsedMs = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
            val remainingMs = (timeoutMs - elapsedMs).coerceAtLeast(0)
            return flushStore(remainingMs)
        }

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> =
            withContext(Dispatchers.IO) {
                require(limit > 0) { "Line limit must be positive" }
                readRecords(limit).map { record ->
                    DiagnosticsLogLine(
                        level = record.level,
                        text = recordEncoder.encode(record),
                        traceId = record.trace.traceId.value,
                        event = record.event.value,
                        errorCode = record.error?.errorCode?.value,
                        exceptionKind =
                            record.error
                                ?.exception
                                ?.kind
                                ?.name,
                        exceptionType =
                            record.error
                                ?.exception
                                ?.type
                                ?.value,
                    )
                }
            }

        override suspend fun clear(): LogClearResult = withContext(Dispatchers.IO) { store.clear() }

        override suspend fun storageBytes(): Long = withContext(Dispatchers.IO) { store.storageBytes() }

        fun shutdown() {
            if (!shutdown.compareAndSet(false, true)) return
            settingsJob?.cancel()
            settingsScope.cancel()
            var failure: Throwable? = null

            fun attemptCleanup(action: () -> Unit) {
                try {
                    action()
                } catch (cleanupFailure: Throwable) {
                    val current = failure
                    failure =
                        when {
                            cleanupFailure is Error && current !is Error -> {
                                current?.let { if (it !== cleanupFailure) cleanupFailure.addSuppressed(it) }
                                cleanupFailure
                            }

                            current == null -> {
                                cleanupFailure
                            }

                            current !== cleanupFailure -> {
                                current.addSuppressed(cleanupFailure)
                                current
                            }

                            else -> {
                                current
                            }
                        }
                }
            }
            attemptCleanup { flush(LIFECYCLE_FLUSH_TIMEOUT_MS) }
            attemptCleanup {
                if (crashHandlers.current() === crashHandler) crashHandlers.install(previousCrashHandler)
            }
            attemptCleanup { store.close() }
            (failure as? Error)?.let { throw it }
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
            try {
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
            } catch (_: Exception) {
                // A previous-process summary is best-effort; fatal Errors remain visible.
            }
        }

        private fun captureFatal(throwable: Throwable): com.aozijx.passly.core.observability.SanitizedLogRecord? {
            crashCapture.clear()
            try {
                runBlocking {
                    logger.withRootTrace(Operations.ApplicationLifecycle) {
                        logger.log(LogStatements.Application.unhandledException(throwable))
                    }
                }
            } catch (loggingFailure: Throwable) {
                addDiagnosticFailure(throwable, loggingFailure)
            }
            return crashCapture.take()
        }

        private fun flushStore(timeoutMs: Long): Boolean =
            try {
                store.flush(timeoutMs)
            } catch (_: Exception) {
                false
            }

        private fun addDiagnosticFailure(
            original: Throwable,
            diagnosticFailure: Throwable,
        ) {
            if (diagnosticFailure !== original) original.addSuppressed(diagnosticFailure)
        }

        private fun delegateCrash(
            thread: Thread,
            throwable: Throwable,
        ) {
            previousCrashHandler?.uncaughtException(thread, throwable) ?: processTerminator.terminate()
        }

        private companion object {
            const val PAGE_SIZE = 500
            const val NANOS_PER_MILLISECOND = 1_000_000L
            const val LIFECYCLE_FLUSH_TIMEOUT_MS = 300L
            const val CRASH_FLUSH_TIMEOUT_MS = 300L
            const val CRASH_EMERGENCY_TIMEOUT_MS = 200L
        }
    }

internal object PlatformCrashHandlerRegistry : CrashHandlerRegistry {
    override fun current(): Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun install(handler: Thread.UncaughtExceptionHandler?) {
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }
}

internal object AndroidProcessTerminator : ProcessTerminator {
    override fun terminate() = Process.killProcess(Process.myPid())
}
