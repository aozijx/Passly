package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.trace.SpanCompleted
import com.aozijx.passly.core.observability.trace.SpanResult
import com.aozijx.passly.core.observability.trace.SpanStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidTraceSinkTest {
    @Test
    fun `async trace begin and end use same fixed name and cookie once`() {
        val gateway = RecordingTraceGateway()
        val sink = AndroidTraceSink(gateway)
        val trace = TraceSnapshot(TraceId("trace_123"), SpanId("span_1234"), null)

        sink.onSpanStarted(SpanStarted(Operations.AutofillResultDelivery, trace, 10))
        sink.onSpanCompleted(SpanCompleted(Operations.AutofillResultDelivery, trace, 20, SpanResult.SUCCESS))
        sink.onSpanCompleted(SpanCompleted(Operations.AutofillResultDelivery, trace, 20, SpanResult.SUCCESS))

        assertEquals(1, gateway.begins.size)
        assertEquals(1, gateway.ends.size)
        assertEquals("Passly/AutofillResult", gateway.begins.single().first)
        assertEquals(gateway.begins.single(), gateway.ends.single())
    }

    @Test
    fun `overlapping spans receive distinct active cookies`() {
        val gateway = RecordingTraceGateway()
        val sink = AndroidTraceSink(gateway, cookieSeed = 7)
        val first = TraceSnapshot(TraceId("trace_123"), SpanId("collision_Aa"), null)
        val second = TraceSnapshot(TraceId("trace_123"), SpanId("collision_BB"), null)

        sink.onSpanStarted(SpanStarted(Operations.DatabaseSession, first, 10))
        sink.onSpanStarted(SpanStarted(Operations.DatabaseSession, second, 11))

        assertEquals(2, gateway.begins.map { it.second }.distinct().size)
    }

    @Test
    fun `trace failure is swallowed without suppressing caller`() {
        val sink = AndroidTraceSink(object : AndroidTraceGateway {
            override fun isEnabled(): Boolean = true
            override fun beginAsyncSection(name: String, cookie: Int) = error("platform unavailable")
            override fun endAsyncSection(name: String, cookie: Int) = error("platform unavailable")
        })
        val trace = TraceSnapshot(TraceId("trace_123"), SpanId("span_1234"), null)

        sink.onSpanStarted(SpanStarted(Operations.ApplicationStartup, trace, 10))
        sink.onSpanCompleted(SpanCompleted(Operations.ApplicationStartup, trace, 20, SpanResult.FAILURE))
        val businessResult = 42

        assertEquals(42, businessResult)
    }

    private class RecordingTraceGateway : AndroidTraceGateway {
        val begins = mutableListOf<Pair<String, Int>>()
        val ends = mutableListOf<Pair<String, Int>>()
        override fun isEnabled(): Boolean = true
        override fun beginAsyncSection(name: String, cookie: Int) {
            begins += name to cookie
        }
        override fun endAsyncSection(name: String, cookie: Int) {
            ends += name to cookie
        }
    }
}
