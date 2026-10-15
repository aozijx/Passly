package com.aozijx.passly.core.observability.trace

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TracePropagationTest {
    @Test
    fun `root and nested spans share trace and maintain direct parent relationship`() = runTest {
        val logger = logger()

        logger.withRootTrace(Operations.ApplicationStartup) {
            val root = TraceContext.currentOrNull()!!
            assertEquals(traceId, root.traceId)
            assertNull(parentSpanId)

            logger.inSpan(Operations.DatabaseSession) {
                assertEquals(root.traceId, traceId)
                assertEquals(root.spanId, parentSpanId)
                val child = TraceContext.currentOrNull()!!

                logger.inSpan(Operations.AutofillResultDelivery) {
                    assertEquals(root.traceId, traceId)
                    assertEquals(child.spanId, parentSpanId)
                }
            }

            assertEquals(root.spanId, TraceContext.currentOrNull()?.spanId)
        }
        assertNull(TraceContext.currentOrNull())
    }

    @Test
    fun `dispatcher switch preserves trace context`() = runTest {
        val logger = logger()

        logger.withRootTrace(Operations.ApplicationStartup) {
            val expected = TraceContext.currentOrNull()
            withContext(Dispatchers.Default) {
                assertEquals(expected, TraceContext.currentOrNull())
            }
        }
    }

    @Test
    fun `cancelled span closes once and restores parent context`() = runTest {
        val lifecycle = RecordingTraceLifecycleSink()
        val logger = logger(lifecycleSink = lifecycle)

        logger.withRootTrace(Operations.ApplicationStartup) {
            val rootSpan = spanId
            val thrown = try {
                logger.inSpan(Operations.DatabaseSession) {
                    throw CancellationException("cancel")
                }
                null
            } catch (cancelled: CancellationException) {
                cancelled
            }
            assertTrue(thrown is CancellationException)
            assertEquals(rootSpan, TraceContext.currentOrNull()?.spanId)
        }

        val childCompletions = lifecycle.completed.filter { it.operation == Operations.DatabaseSession }
        assertEquals(1, childCompletions.size)
        assertEquals(SpanResult.CANCELLED, childCompletions.single().result)
    }

    @Test
    fun `exceptional span records elapsed time and rethrows the original exception`() = runTest {
        val timeSource = FakeTimeSource()
        val lifecycle = RecordingTraceLifecycleSink()
        val logger = logger(timeSource, lifecycle)
        val failure = IllegalArgumentException("do not replace")

        logger.withRootTrace(Operations.ApplicationStartup) {
            val thrown = try {
                logger.inSpan(Operations.DatabaseSession) {
                    timeSource.elapsedMs = 37
                    throw failure
                }
                null
            } catch (caught: IllegalArgumentException) {
                caught
            }
            assertSame(failure, thrown)
        }

        val completion = lifecycle.completed.single { it.operation == Operations.DatabaseSession }
        assertEquals(37, completion.elapsedMs)
        assertEquals(SpanResult.FAILURE, completion.result)
    }

    @Test
    fun `expired or unknown trace token cannot attach to another session`() {
        var nowMs = 100L
        var tokenCounter = 0
        val registry = TraceTokenRegistry(
            nowMs = { nowMs },
            tokenFactory = { TraceToken("token_${++tokenCounter}_value") },
            ttlMs = 600_000,
            capacity = 2,
        )
        val process = ProcessSessionId("process_session_a")
        val context = TraceContext.root(
            TraceId("trace_context_a"),
            SpanId("span_context_a"),
            Operations.AutofillLegacyRequest,
        )

        val wrongSessionToken = registry.capture(context, process)
        assertNull(registry.consume(wrongSessionToken, ProcessSessionId("process_session_b")))
        assertNull(registry.consume(wrongSessionToken, process))

        val expiredToken = registry.capture(context, process)
        nowMs += 600_001
        assertNull(registry.consume(expiredToken, process))
        assertNull(registry.consume(TraceToken("unknown_token_value"), process))

        val validToken = registry.capture(context, process)
        val resumed = registry.consume(validToken, process)
        assertEquals(context.traceId, resumed?.traceId)
        assertEquals(context.spanId, resumed?.spanId)
        assertNull(registry.consume(validToken, process))
    }

    @Test
    fun `trace token registry evicts the oldest entry at its fixed capacity`() {
        var tokenCounter = 0
        val registry = TraceTokenRegistry(
            nowMs = { 100 },
            tokenFactory = { TraceToken("token_${++tokenCounter}_value") },
            capacity = 2,
        )
        val process = ProcessSessionId("process_session_a")
        val context = TraceContext.root(
            TraceId("trace_context_a"),
            SpanId("span_context_a"),
            Operations.ApplicationStartup,
        )

        val first = registry.capture(context, process)
        val second = registry.capture(context, process)
        val third = registry.capture(context, process)

        assertNull(registry.consume(first, process))
        assertTrue(registry.consume(second, process) != null)
        assertTrue(registry.consume(third, process) != null)
    }

    @Test
    fun `captured trace token resumes as a child span on the original trace`() = runTest {
        val lifecycle = RecordingTraceLifecycleSink()
        val logger = logger(lifecycleSink = lifecycle)
        var token: TraceToken? = null
        var rootTraceId: TraceId? = null
        var rootSpanId: SpanId? = null

        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            rootTraceId = traceId
            rootSpanId = spanId
            token = logger.captureTraceToken()
        }

        val result = logger.resumeTrace(token!!, Operations.AutofillResultDelivery) {
            assertEquals(rootTraceId, traceId)
            assertEquals(rootSpanId, parentSpanId)
            "delivered"
        }

        assertEquals(TraceResumeResult.Resumed("delivered"), result)
        val resumed = lifecycle.started.single { it.operation == Operations.AutofillResultDelivery }
        assertEquals(rootTraceId, resumed.trace.traceId)
        assertEquals(rootSpanId, resumed.trace.parentSpanId)
    }

    @Test
    fun `single use trace token cannot be resumed twice`() = runTest {
        val logger = logger()
        var token: TraceToken? = null

        logger.withRootTrace(Operations.AutofillCredentialRequest) {
            token = logger.captureTraceToken()
        }

        val captured = requireNotNull(token)
        assertTrue(logger.resumeTrace(captured, Operations.AutofillResultDelivery) { Unit } is TraceResumeResult.Resumed)
        assertEquals(
            TraceResumeResult.Unavailable,
            logger.resumeTrace(captured, Operations.AutofillSessionClose) { Unit },
        )
    }

    @Test
    fun `continuation store consumes an opaque callback key without logging it`() = runTest {
        val lifecycle = RecordingTraceLifecycleSink()
        val logger = logger(lifecycleSink = lifecycle)
        val continuations = TraceContinuationStore(logger)
        var originalTrace: TraceId? = null

        logger.withRootTrace(Operations.AutofillCredentialRequest) {
            originalTrace = traceId
            continuations.capture("interaction_1234")
        }

        val resumed = continuations.resume("interaction_1234", Operations.AutofillResultDelivery) {
            traceId
        }

        assertEquals(TraceResumeResult.Resumed(originalTrace), resumed)
        assertEquals(
            TraceResumeResult.Unavailable,
            continuations.resume("interaction_1234", Operations.AutofillSessionClose) { traceId },
        )
    }

    @Test
    fun `trace lifecycle sink failure cannot suppress business execution`() = runTest {
        val throwingSink = object : TraceLifecycleSink {
            override fun onSpanStarted(span: SpanStarted) = error("trace start unavailable")
            override fun onSpanCompleted(span: SpanCompleted) = error("trace end unavailable")
        }
        val logger = logger(lifecycleSink = throwingSink)

        val result = logger.withRootTrace(Operations.ApplicationStartup) { 42 }

        assertEquals(42, result)
    }

    private fun logger(
        timeSource: FakeTimeSource = FakeTimeSource(),
        lifecycleSink: TraceLifecycleSink = RecordingTraceLifecycleSink(),
    ) = DefaultLogger(
        sanitizer = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT),
        sinks = emptyList(),
        lifecycleSinks = listOf(lifecycleSink),
        idGenerator = SequentialIdGenerator(),
        timeSource = timeSource,
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { SecureSessionId("secure_session") },
    )

    private class FakeTimeSource(
        var elapsedMs: Long = 0,
    ) : ObservabilityTimeSource {
        override fun wallTimeMs(): Long = 1_700_000_000_000
        override fun elapsedRealtimeMs(): Long = elapsedMs
    }

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0
        private fun id(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(id("event"))
        override fun traceId() = TraceId(id("trace"))
        override fun spanId() = SpanId(id("span"))
        override fun diagnosticId() =
            DiagnosticId.fromEntropy(ByteArray(8) { index -> (index + next++).toByte() })
    }

    private class RecordingTraceLifecycleSink : TraceLifecycleSink {
        val started = mutableListOf<SpanStarted>()
        val completed = mutableListOf<SpanCompleted>()
        override fun onSpanStarted(span: SpanStarted) {
            started += span
        }
        override fun onSpanCompleted(span: SpanCompleted) {
            completed += span
        }
    }
}
