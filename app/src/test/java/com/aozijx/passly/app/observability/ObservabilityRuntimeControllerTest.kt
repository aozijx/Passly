package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.*
import com.aozijx.passly.core.observability.internal.*
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.privacy.*
import com.aozijx.passly.core.observability.sink.*
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class ObservabilityRuntimeControllerTest {
    @Test
    fun `startup installs one crash handler then records previous exit`() {
        val order = mutableListOf<String>()
        val store = RecordingStore(order = order)
        val previous = PreviousExitSource {
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
        val previous = PreviousExitSource {
            previousExitReads.set(true)
            PreviousExit(1, PreviousExitReason.CRASH, 9, PreviousExitImportance.FOREGROUND, 2, 3)
        }
        val delayedMode = flow {
            modeCollectionStarted.countDown()
            releaseMode.await(2, TimeUnit.SECONDS)
            emit(DiagnosticsLogRecordingMode.INFO_AND_ABOVE)
        }
        val store = RecordingStore()
        val controller = controller(
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
    fun `reentrant crash delegates without a second fatal or emergency write`() {
        val store = RecordingStore(flushResult = false)
        var delegated = 0
        val registry = RecordingCrashHandlerRegistry(
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
        val controller = controller(store, noPreviousExit, RecordingCrashHandlerRegistry()) { logger ->
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
        val store = RecordingStore().apply {
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
        beforeController: (Logger) -> Unit = {},
    ): ObservabilityRuntimeController {
        val crashCapture = CrashRecordCaptureSink()
        val logger = DefaultLogger(
            sanitizer = PrivacyPolicy(
                IdentifierHasher { _, _ -> null },
                ContractViolationBehavior.THROW,
            ),
            sinks = listOf(crashCapture, store),
            lifecycleSinks = emptyList(),
            idGenerator = SequentialIdGenerator(),
            timeSource = object : ObservabilityTimeSource {
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
            diagnosticsLogSettings = object : DiagnosticsLogSettingsRepository {
                override val diagnosticsLogRecordingMode = recordingMode
                override suspend fun setDiagnosticsLogRecordingMode(mode: DiagnosticsLogRecordingMode) = Unit
            },
            recordingPolicy = DiagnosticsLogRecordingPolicy(),
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
        override fun write(record: SanitizedLogRecord) {
            records += record
            order += "log_write"
            logWritten.countDown()
        }
        override fun readPage(cursor: LogRecordCursor?, limit: Int) =
            LogRecordPage(emptyList(), 0, null)
        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) =
            emptyList<SanitizedLogRecord>()
        override fun storageBytes() = 0L
        override fun drainDroppedCounts() = dropped.also { dropped = LogDropCounts() }
        override fun flush(timeoutMs: Long): Boolean {
            flushTimeouts += timeoutMs
            onFlush()
            return flushResult
        }
        override fun clear() = LogClearResult.Cleared
        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean {
            emergencyTimeouts += timeoutMs
            return true
        }
        override fun close() { closeCalls++ }
    }
}
