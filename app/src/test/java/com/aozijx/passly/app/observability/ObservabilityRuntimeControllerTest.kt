package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.FatalLogStatement
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogStatement
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordPage
import com.aozijx.passly.core.observability.sink.LogRecordStore
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservabilityRuntimeControllerTest {
    @Test
    fun `startup installs runtime then one crash handler then records previous exit`() {
        val order = mutableListOf<String>()
        val store = RecordingStore()
        val logger = RecordingFrameworkLogger(order)
        val runtime = RecordingRuntimeGateway(order)
        val previous = PreviousExitSource {
            order += "previous_exit_read"
            PreviousExit(1, PreviousExitReason.CRASH, 9, PreviousExitImportance.FOREGROUND, 2, 3)
        }
        val registry = RecordingCrashHandlerRegistry(order)
        val controller = controller(store, logger, runtime, previous, registry)

        controller.start()
        controller.start()

        assertEquals(
            listOf("runtime_install", "handler_install", "previous_exit_read", "info"),
            order,
        )
        assertEquals(1, runtime.installCalls)
        assertEquals(1, registry.installCalls)
        assertEquals(Events.Application.PreviousProcessExit, logger.infoEvents.single())
    }

    @Test
    fun `crash writes fatal flushes for 300 ms falls back for 200 ms and delegates`() {
        val store = RecordingStore(flushResult = false)
        val logger = RecordingFrameworkLogger()
        var delegated: Pair<Thread, Throwable>? = null
        val previous = Thread.UncaughtExceptionHandler { thread, error -> delegated = thread to error }
        val registry = RecordingCrashHandlerRegistry(previous = previous)
        val controller = controller(store, logger, RecordingRuntimeGateway(), noPreviousExit, registry)
        controller.start()
        val error = IllegalStateException("must not be logged")

        registry.current!!.uncaughtException(Thread.currentThread(), error)

        assertEquals(1, logger.fatalCalls)
        assertEquals(listOf(300L), store.flushTimeouts)
        assertEquals(listOf(200L), store.emergencyTimeouts)
        assertSame(error, delegated!!.second)
    }

    @Test
    fun `reentrant crash delegates without a second log or emergency write`() {
        val store = RecordingStore(flushResult = false)
        val logger = RecordingFrameworkLogger()
        var delegated = 0
        val registry = RecordingCrashHandlerRegistry(
            previous = Thread.UncaughtExceptionHandler { _, _ -> delegated++ },
        )
        val controller = controller(store, logger, RecordingRuntimeGateway(), noPreviousExit, registry)
        controller.start()
        val nested = AtomicBoolean(false)
        store.onFlush = {
            if (nested.compareAndSet(false, true)) {
                registry.current!!.uncaughtException(Thread.currentThread(), RuntimeException("nested"))
            }
        }

        registry.current!!.uncaughtException(Thread.currentThread(), RuntimeException("outer"))

        assertEquals(1, logger.fatalCalls)
        assertEquals(1, store.emergencyTimeouts.size)
        assertEquals(2, delegated)
    }

    @Test
    fun `lifecycle flush and shutdown are bounded and idempotent`() {
        val store = RecordingStore()
        val runtime = RecordingRuntimeGateway()
        val previous = Thread.UncaughtExceptionHandler { _, _ -> }
        val registry = RecordingCrashHandlerRegistry(previous = previous)
        val controller = controller(store, RecordingFrameworkLogger(), runtime, noPreviousExit, registry)
        controller.start()

        assertTrue(controller.flush())
        controller.shutdown()
        controller.shutdown()

        assertEquals(listOf(300L), store.flushTimeouts)
        assertEquals(1, store.closeCalls)
        assertEquals(1, runtime.uninstallCalls)
        assertSame(previous, registry.current)
    }

    private fun controller(
        store: RecordingStore,
        logger: RecordingFrameworkLogger,
        runtime: RecordingRuntimeGateway,
        previous: PreviousExitSource,
        registry: RecordingCrashHandlerRegistry,
    ) = ObservabilityRuntimeController(
        store = store,
        frameworkLogger = logger,
        loggerRuntime = runtime,
        previousExitSource = previous,
        crashHandlers = registry,
        processTerminator = ProcessTerminator { },
    )

    private val noPreviousExit = PreviousExitSource { null }

    private class RecordingFrameworkLogger(
        private val order: MutableList<String> = mutableListOf(),
    ) : FrameworkLogger {
        val infoEvents = mutableListOf<EventKey>()
        var fatalCalls = 0

        override fun log(operation: OperationCode, statement: LogStatement) {
            order += "info"
            infoEvents += statement.event
        }

        override fun fatal(
            operation: OperationCode,
            statement: FatalLogStatement,
        ): SanitizedLogRecord? {
            fatalCalls++
            return fatalRecord()
        }
    }

    private class RecordingRuntimeGateway(
        private val order: MutableList<String> = mutableListOf(),
    ) : LoggerRuntimeGateway {
        var installCalls = 0
        var uninstallCalls = 0

        override fun install(logger: FrameworkLogger) {
            installCalls++
            order += "runtime_install"
        }

        override fun uninstall(logger: FrameworkLogger) {
            uninstallCalls++
        }
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
    ) : LogRecordStore {
        val flushTimeouts = mutableListOf<Long>()
        val emergencyTimeouts = mutableListOf<Long>()
        var closeCalls = 0
        var onFlush: () -> Unit = {}

        override fun write(record: SanitizedLogRecord) = Unit
        override fun readPage(cursor: LogRecordCursor?, limit: Int) =
            LogRecordPage(emptyList(), 0, null)
        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) =
            emptyList<SanitizedLogRecord>()
        override fun flush(timeoutMs: Long): Boolean {
            flushTimeouts += timeoutMs
            onFlush()
            return flushResult
        }
        override fun clear() = Unit
        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean {
            emergencyTimeouts += timeoutMs
            return true
        }
        override fun close() { closeCalls++ }
    }

    private companion object {
        fun fatalRecord() = SanitizedLogRecord(
            eventId = EventId("event_crash_123"),
            timestampMs = 1,
            elapsedRealtimeMs = 2,
            level = LogLevel.FATAL,
            category = LogCategory.APPLICATION,
            event = Events.Application.UnhandledException,
            processSessionId = ProcessSessionId("process_123456"),
            secureSessionId = null,
            trace = TraceSnapshot(TraceId("trace_12345678"), SpanId("span_123456789"), null),
            fields = LogFields.Empty,
            error = ErrorPayload(
                ErrorCodes.ApplicationUnhandledException,
                Reasons.UnknownFailure,
                DiagnosticId("DGN-ABCD-EFGH"),
                ExceptionSnapshot(ExceptionType("IllegalStateException"), emptyList(), emptyList(), 0),
            ),
        )
    }
}
