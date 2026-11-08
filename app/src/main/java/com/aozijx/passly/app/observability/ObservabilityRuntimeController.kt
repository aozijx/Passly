package com.aozijx.passly.app.observability

import android.os.Process
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.app.settings.SettingsReadRetrySignal
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsSource
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

private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val LIFECYCLE_FLUSH_TIMEOUT_MS = 300L
private const val CRASH_FLUSH_TIMEOUT_MS = 300L
private const val CRASH_EMERGENCY_TIMEOUT_MS = 200L

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
        private val diagnosticsLogSettings: DiagnosticsLogSettingsSource,
        private val recordingPolicy: DiagnosticsLogRecordingPolicy,
        retrySignal: SettingsReadRetrySignal,
    ) {
        private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val settingsRead =
            RecoverableRead(
                source = diagnosticsLogSettings.diagnosticsLogRecordingMode,
                scope = settingsScope,
                sharedRetrySignal = retrySignal,
            )
        private val started = AtomicBoolean(false)
        private val previousExitProcessed = AtomicBoolean(false)
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
                    settingsRead.state.collect { state ->
                        when (state) {
                            RecoverableReadState.Loading,
                            is RecoverableReadState.Failed,
                            -> {
                                recordingPolicy.update(null)
                            }

                            is RecoverableReadState.Ready -> {
                                recordingPolicy.update(state.value)
                                if (previousExitProcessed.compareAndSet(false, true)) {
                                    previousExitSource.readUnrecorded()?.let(::recordPreviousExit)
                                }
                            }
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
