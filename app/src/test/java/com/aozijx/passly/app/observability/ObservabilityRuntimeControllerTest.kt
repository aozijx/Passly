package com.aozijx.passly.app.observability

import com.aozijx.passly.app.settings.SettingsReadRetrySignal
import com.aozijx.passly.core.observability.*
import com.aozijx.passly.core.observability.internal.*
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.privacy.*
import com.aozijx.passly.core.observability.sink.*
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsSource
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ObservabilityRuntimeControllerTest {
    @Test
    fun `startup installs one crash handler then records previous exit`() {
        val order = mutableListOf<String>()
        val store = RecordingStore(order = order)
        val previous =
            PreviousExitSource {
                order += "previous_exit_read"
                PreviousExit(1, PreviousExitReason.CRASH, 9, PreviousExitImportance.FOREGROUND, 2, 3)
            }
        val registry = RecordingCrashHandlerRegistry(order)
        val controller = controller(store, previous, registry)

        controller.start()
        controller.start()
        assertTrue(store.logWritten.await(2, TimeUnit.SECONDS))

        assertEquals(listOf("handler_install", "previous_exit_read", "log_write"), order)
        assertEquals(1, registry.installCalls)
        assertEquals(Events.Application.PreviousProcessExit, store.records.single().event)
    }

    @Test
    fun `previous exit is not consumed until persistent recording mode is loaded`() {
        val modeCollectionStarted = CountDownLatch(1)
        val releaseMode = CountDownLatch(1)
        val previousExitReads = AtomicBoolean(false)
        val previous =
            PreviousExitSource {
                previousExitReads.set(true)
                PreviousExit(1, PreviousExitReason.CRASH, 9, PreviousExitImportance.FOREGROUND, 2, 3)
            }
        val delayedMode =
            flow {
                modeCollectionStarted.countDown()
                releaseMode.await(2, TimeUnit.SECONDS)
                emit(DiagnosticsLogRecordingMode.INFO_AND_ABOVE)
            }
        val store = RecordingStore()
        val controller =
            controller(
                store = store,
                previous = previous,
                registry = RecordingCrashHandlerRegistry(),
                recordingMode = delayedMode,
            )

        try {
            controller.start()
            assertTrue(modeCollectionStarted.await(2, TimeUnit.SECONDS))
            assertFalse(previousExitReads.get())
            assertTrue(store.records.isEmpty())

            releaseMode.countDown()
            assertTrue(store.logWritten.await(2, TimeUnit.SECONDS))
            assertTrue(previousExitReads.get())
            assertEquals(Events.Application.PreviousProcessExit, store.records.single().event)
        } finally {
            releaseMode.countDown()
            controller.shutdown()
        }
    }

    @Test
    fun `diagnostics source recovers after read failures and consumes previous exit once`() {
        val attempts = AtomicInteger()
        val previousReads = AtomicInteger()
        val recoveredModes = CountDownLatch(2)
        val previous =
            PreviousExitSource {
                previousReads.incrementAndGet()
                PreviousExit(1, PreviousExitReason.CRASH, 9, PreviousExitImportance.FOREGROUND, 2, 3)
            }
        val recordingPolicy = DiagnosticsLogRecordingPolicy()
        val mode =
            flow {
                when (attempts.incrementAndGet()) {
                    1 -> {
                        throw IOException("initial read failed")
                    }

                    2 -> {
                        emit(DiagnosticsLogRecordingMode.INFO_AND_ABOVE)
                        recoveredModes.countDown()
                        throw IOException("read failed after value")
                    }

                    else -> {
                        emit(DiagnosticsLogRecordingMode.DISABLED)
                        recoveredModes.countDown()
                        awaitCancellation()
                    }
                }
            }
        val store = RecordingStore()
        val controller =
            controller(
                store = store,
                previous = previous,
                registry = RecordingCrashHandlerRegistry(),
                recordingMode = mode,
                recordingPolicy = recordingPolicy,
            )

        try {
            controller.start()

            assertTrue(recoveredModes.await(5, TimeUnit.SECONDS))
            assertTrue(store.logWritten.await(2, TimeUnit.SECONDS))
            assertEquals(1, previousReads.get())
            assertEquals(1, store.records.count { it.event == Events.Application.PreviousProcessExit })
            assertFalse(recordingPolicy.allows(LogLevel.INFO))
        } finally {
            controller.shutdown()
        }
    }

    @Test
    fun `crash writes fatal flushes for 300 ms falls back for 200 ms and delegates`() {
        val store = RecordingStore(flushResult = false)
        var delegated: Pair<Thread, Throwable>? = null
        val previous = Thread.UncaughtExceptionHandler { thread, error -> delegated = thread to error }
        val registry = RecordingCrashHandlerRegistry(previous = previous)
        val controller = controller(store, noPreviousExit, registry)
        controller.start()
        val error = IllegalStateException("must not be logged")

        registry.current!!.uncaughtException(Thread.currentThread(), error)

        assertEquals(1, store.records.count { it.level == LogLevel.FATAL })
        assertEquals(listOf(300L), store.flushTimeouts)
        assertEquals(listOf(200L), store.emergencyTimeouts)
        assertSame(error, delegated!!.second)
    }

    @Test
    fun `crash diagnostics failure stays suppressed under original crash`() {
        val store = RecordingStore(flushResult = true)
        val diagnosticFailure = AssertionError("diagnostic flush fatal")
        store.flushFailure = diagnosticFailure
        var delegated: Throwable? = null
        val registry =
            RecordingCrashHandlerRegistry(
                previous = Thread.UncaughtExceptionHandler { _, error -> delegated = error },
            )
        val controller = controller(store, noPreviousExit, registry)
        controller.start()
        val original = IllegalStateException("original crash")

        registry.current!!.uncaughtException(Thread.currentThread(), original)

        assertSame(original, delegated)
        assertTrue(original.suppressed.contains(diagnosticFailure))
        assertEquals(listOf(200L), store.emergencyTimeouts)
    }

    @Test
    fun `lifecycle flush propagates fatal storage error`() {
        val store = RecordingStore().apply { flushFailure = AssertionError("flush fatal") }
        val controller = controller(store, noPreviousExit, RecordingCrashHandlerRegistry())

        val thrown =
            try {
                controller.flush()
                null
            } catch (failure: AssertionError) {
                failure
            }

        assertEquals("flush fatal", thrown?.message)
    }

    @Test
    fun `shutdown propagates fatal close error after restoring crash handler`() {
        val fatal = AssertionError("close fatal")
        val store = RecordingStore().apply { closeFailure = fatal }
        val previous = Thread.UncaughtExceptionHandler { _, _ -> }
        val registry = RecordingCrashHandlerRegistry(previous = previous)
        val controller = controller(store, noPreviousExit, registry)
        controller.start()

        val thrown =
            try {
                controller.shutdown()
                null
            } catch (failure: AssertionError) {
                failure
            }

        assertSame(fatal, thrown)
        assertSame(previous, registry.current)
    }

    @Test
    fun `reentrant crash delegates without a second fatal or emergency write`() {
        val store = RecordingStore(flushResult = false)
        var delegated = 0
        val registry =
            RecordingCrashHandlerRegistry(
                previous = Thread.UncaughtExceptionHandler { _, _ -> delegated++ },
            )
        val controller = controller(store, noPreviousExit, registry)
        controller.start()
        val nested = AtomicBoolean(false)
        store.onFlush = {
            if (nested.compareAndSet(false, true)) {
                registry.current!!.uncaughtException(Thread.currentThread(), RuntimeException("nested"))
            }
        }

        registry.current!!.uncaughtException(Thread.currentThread(), RuntimeException("outer"))

        assertEquals(1, store.records.count { it.level == LogLevel.FATAL })
        assertEquals(1, store.emergencyTimeouts.size)
        assertEquals(2, delegated)
    }

    @Test
    fun `lifecycle flush and shutdown are bounded and idempotent`() {
        val store = RecordingStore()
        val previous = Thread.UncaughtExceptionHandler { _, _ -> }
        val registry = RecordingCrashHandlerRegistry(previous = previous)
        val controller = controller(store, noPreviousExit, registry)
        controller.start()

        assertTrue(controller.flush())
        controller.shutdown()
        controller.shutdown()

        assertEquals(listOf(300L, 300L), store.flushTimeouts)
        assertEquals(1, store.closeCalls)
        assertSame(previous, registry.current)
    }

    @Test
    fun `logger accepts startup events before runtime controller starts`() {
        val store = RecordingStore()
        val controller =
            controller(store, noPreviousExit, RecordingCrashHandlerRegistry()) { logger ->
                runBlocking {
                    logger.withRootTrace(Operations.ApplicationStartup) {
                        logger.log(LogStatements.Application.traceResumeUnavailable())
                    }
                }
            }

        assertEquals(listOf(Events.Application.TraceResumeUnavailable), store.records.map { it.event })
        controller.start()
    }

    @Test
    fun `flush records one structured overflow summary and drains counters`() {
        val store =
            RecordingStore().apply {
                dropped = LogDropCounts(debug = 4, info = 2, warn = 1)
            }
        val controller = controller(store, noPreviousExit, RecordingCrashHandlerRegistry())

        assertTrue(controller.flush())

        val overflow = store.records.single { it.event == Events.Application.LogOverflow }
        assertEquals(LogValue.Count(4), overflow.fields.values[LogFieldKeys.DroppedDebugCount])
        assertEquals(LogValue.Count(2), overflow.fields.values[LogFieldKeys.DroppedInfoCount])
        assertEquals(LogValue.Count(1), overflow.fields.values[LogFieldKeys.DroppedWarnCount])
        assertEquals(LogDropCounts(), store.dropped)
        assertEquals(2, store.flushTimeouts.size)
    }

    private fun controller(
        store: RecordingStore,
        previous: PreviousExitSource,
        registry: RecordingCrashHandlerRegistry,
        recordingMode: Flow<DiagnosticsLogRecordingMode> = flowOf(DiagnosticsLogRecordingMode.DEFAULT),
        recordingPolicy: DiagnosticsLogRecordingPolicy = DiagnosticsLogRecordingPolicy(),
        beforeController: (Logger) -> Unit = {},
    ): ObservabilityRuntimeController {
        val crashCapture = CrashRecordCaptureSink()
        val logger =
            DefaultLogger(
                sanitizer =
                    PrivacyPolicy(
                        IdentifierHasher { _, _ -> null },
                        ContractViolationBehavior.THROW,
                    ),
                sinks = listOf(crashCapture, store),
                lifecycleSinks = emptyList(),
                idGenerator = SequentialIdGenerator(),
                timeSource =
                    object : ObservabilityTimeSource {
                        override fun wallTimeMs() = 1_700_000_000_000L

                        override fun elapsedRealtimeMs() = 100L
                    },
                processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
                secureSessionProvider = SecureSessionProvider { null },
            )
        beforeController(logger)
        return ObservabilityRuntimeController(
            store = store,
            logger = logger,
            crashCapture = crashCapture,
            previousExitSource = previous,
            crashHandlers = registry,
            processTerminator = ProcessTerminator { },
            eventAggregator = EventAggregator(),
            diagnosticsLogSettings =
                object : DiagnosticsLogSettingsSource {
                    override val diagnosticsLogRecordingMode = recordingMode
                },
            recordingPolicy = recordingPolicy,
            retrySignal = SettingsReadRetrySignal(),
        )
    }

    private val noPreviousExit = PreviousExitSource { null }

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0

        private fun id(prefix: String) = "${prefix}_${++next}_value"

        override fun eventId() = EventId(id("event"))

        override fun traceId() = TraceId(id("trace"))

        override fun spanId() = SpanId(id("span"))
    }

    private class RecordingCrashHandlerRegistry(
        private val order: MutableList<String> = mutableListOf(),
        previous: Thread.UncaughtExceptionHandler? = null,
    ) : CrashHandlerRegistry {
        var current: Thread.UncaughtExceptionHandler? = previous
        var installCalls = 0

        override fun current(): Thread.UncaughtExceptionHandler? = current

        override fun install(handler: Thread.UncaughtExceptionHandler?) {
            current = handler
            installCalls++
            if (handler != null) order += "handler_install"
        }
    }

    private class RecordingStore(
        private val flushResult: Boolean = true,
        private val order: MutableList<String> = mutableListOf(),
    ) : LogRecordStore {
        val records = mutableListOf<SanitizedLogRecord>()
        val logWritten = CountDownLatch(1)
        val flushTimeouts = mutableListOf<Long>()
        val emergencyTimeouts = mutableListOf<Long>()
        var closeCalls = 0
        var onFlush: () -> Unit = {}
        var dropped = LogDropCounts()
        var flushFailure: Throwable? = null
        var emergencyFailure: Throwable? = null
        var closeFailure: Throwable? = null

        override fun write(record: SanitizedLogRecord) {
            records += record
            order += "log_write"
            logWritten.countDown()
        }

        override fun readPage(
            cursor: LogRecordCursor?,
            limit: Int,
        ) = LogRecordPage(emptyList(), 0, null)

        override fun readBetween(
            startTimestampMs: Long,
            endTimestampMs: Long,
            limit: Int,
        ) = emptyList<SanitizedLogRecord>()

        override fun storageBytes() = 0L

        override fun drainDroppedCounts() = dropped.also { dropped = LogDropCounts() }

        override fun flush(timeoutMs: Long): Boolean {
            flushTimeouts += timeoutMs
            onFlush()
            flushFailure?.let { throw it }
            return flushResult
        }

        override fun clear() = LogClearResult.Cleared

        override fun crashEmergencyWrite(
            record: SanitizedLogRecord,
            timeoutMs: Long,
        ): Boolean {
            emergencyTimeouts += timeoutMs
            emergencyFailure?.let { throw it }
            return true
        }

        override fun close() {
            closeCalls++
            closeFailure?.let { throw it }
        }
    }
}
