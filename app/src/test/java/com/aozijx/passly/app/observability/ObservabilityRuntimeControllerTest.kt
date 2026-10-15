package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.*
import com.aozijx.passly.core.observability.internal.*
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.privacy.*
import com.aozijx.passly.core.observability.sink.*
import java.util.concurrent.atomic.AtomicBoolean
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

        assertEquals(listOf("handler_install", "previous_exit_read", "log_write"), order)
        assertEquals(1, registry.installCalls)
        assertEquals(Events.Application.PreviousProcessExit, store.records.single().event)
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

        assertEquals(listOf(300L), store.flushTimeouts)
        assertEquals(1, store.closeCalls)
        assertSame(previous, registry.current)
    }

    private fun controller(
        store: RecordingStore,
        previous: PreviousExitSource,
        registry: RecordingCrashHandlerRegistry,
    ): ObservabilityRuntimeController {
        val crashCapture = CrashRecordCaptureSink()
        return ObservabilityRuntimeController(
            store = store,
            logger = DefaultLogger(
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
            ),
            crashCapture = crashCapture,
            previousExitSource = previous,
            crashHandlers = registry,
            processTerminator = ProcessTerminator { },
            eventAggregator = EventAggregator(),
        )
    }

    private val noPreviousExit = PreviousExitSource { null }

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0
        private fun id(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(id("event"))
        override fun traceId() = TraceId(id("trace"))
        override fun spanId() = SpanId(id("span"))
        override fun diagnosticId() =
            DiagnosticId.fromEntropy(ByteArray(8) { index -> (index + ++next).toByte() })
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
        val flushTimeouts = mutableListOf<Long>()
        val emergencyTimeouts = mutableListOf<Long>()
        var closeCalls = 0
        var onFlush: () -> Unit = {}
        override fun write(record: SanitizedLogRecord) {
            records += record
            order += "log_write"
        }
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
}
