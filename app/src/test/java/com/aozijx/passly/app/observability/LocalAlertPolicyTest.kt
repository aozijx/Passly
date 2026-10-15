package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.*
import org.junit.Assert.*
import org.junit.Test

class LocalAlertPolicyTest {
    @Test
    fun `three database open failures create one alert with latest trace id`() {
        val policy = LocalAlertPolicy()
        assertNull(policy.observe(record(0, "trace_aaaaaaaa")))
        assertNull(policy.observe(record(1, "trace_bbbbbbbb")))
        val alert = policy.observe(record(2, "trace_cccccccc"))
        assertEquals(TraceId("trace_cccccccc"), alert?.traceId)
        assertNull(policy.observe(record(3, "trace_dddddddd")))
        assertSame(alert, policy.activeAlert(3))
    }

    @Test
    fun `alert and failure window expire after ten minutes`() {
        val policy = LocalAlertPolicy()
        policy.observe(record(0, "trace_aaaaaaaa"))
        policy.observe(record(1, "trace_bbbbbbbb"))
        policy.observe(record(2, "trace_cccccccc"))
        assertNull(policy.activeAlert(600_003))
        assertNull(policy.observe(record(600_004, "trace_dddddddd")))
    }

    private fun record(timestamp: Long, traceId: String) = SanitizedLogRecord(
        eventId = EventId("event_${timestamp}_value"),
        timestampMs = timestamp,
        elapsedRealtimeMs = timestamp,
        level = LogLevel.ERROR,
        category = LogCategory.DATABASE,
        event = Events.Database.SessionTransitionFailed,
        processSessionId = ProcessSessionId("process_123456"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId(traceId), SpanId("span_123456789"), null),
        fields = LogFields.Empty,
        error = ErrorPayload(
            ErrorCodes.DatabaseSessionOpenFailed,
            Reasons.SessionOpenFailed,
            null,
        ),
    )
}
