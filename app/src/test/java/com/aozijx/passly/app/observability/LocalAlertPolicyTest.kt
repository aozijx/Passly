package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.*
import org.junit.Assert.*
import org.junit.Test

class LocalAlertPolicyTest {
    @Test
    fun `three database open failures create one alert with latest diagnostic id`() {
        val policy = LocalAlertPolicy()
        assertNull(policy.observe(record(0, "DGN-AAAA-AAAA")))
        assertNull(policy.observe(record(1, "DGN-BBBB-BBBB")))
        val alert = policy.observe(record(2, "DGN-CCCC-CCCC"))
        assertEquals(DiagnosticId("DGN-CCCC-CCCC"), alert?.diagnosticId)
        assertNull(policy.observe(record(3, "DGN-DDDD-DDDD")))
        assertSame(alert, policy.activeAlert(3))
    }

    @Test
    fun `alert and failure window expire after ten minutes`() {
        val policy = LocalAlertPolicy()
        policy.observe(record(0, "DGN-AAAA-AAAA"))
        policy.observe(record(1, "DGN-BBBB-BBBB"))
        policy.observe(record(2, "DGN-CCCC-CCCC"))
        assertNull(policy.activeAlert(600_003))
        assertNull(policy.observe(record(600_004, "DGN-DDDD-DDDD")))
    }

    private fun record(timestamp: Long, diagnosticId: String) = SanitizedLogRecord(
        eventId = EventId("event_${timestamp}_value"),
        timestampMs = timestamp,
        elapsedRealtimeMs = timestamp,
        level = LogLevel.ERROR,
        category = LogCategory.DATABASE,
        event = Events.Database.SessionTransitionFailed,
        processSessionId = ProcessSessionId("process_123456"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId("trace_12345678"), SpanId("span_123456789"), null),
        fields = LogFields.Empty,
        error = ErrorPayload(
            ErrorCodes.DatabaseSessionOpenFailed,
            Reasons.SessionOpenFailed,
            DiagnosticId(diagnosticId),
            null,
        ),
    )
}
