package com.aozijx.passly.core.observability.metrics

import com.aozijx.passly.core.observability.*
import org.junit.Assert.*
import org.junit.Test

class EventAggregatorTest {
    @Test
    fun `safe dimensions aggregate while trace ids do not split buckets`() {
        val aggregator = EventAggregator(capacity = 8, debugSamplePercent = 100)
        aggregator.record(record(timestamp = 10, trace = "trace_aaaaaaaa", failure = true))
        aggregator.record(record(timestamp = 20, trace = "trace_bbbbbbbb", failure = true))

        val metric = aggregator.flush().single()
        assertEquals(2L, metric.count)
        assertEquals(10L, metric.firstTimestampMs)
        assertEquals(20L, metric.lastTimestampMs)
        assertEquals(7L, metric.minElapsedMs)
        assertEquals(7L, metric.maxElapsedMs)
        assertEquals(7.0, metric.averageElapsedMs!!, 0.0)
    }

    @Test
    fun `lru eviction and window flush emit bounded snapshots`() {
        val aggregator = EventAggregator(capacity = 2, windowMs = 5, debugSamplePercent = 100)
        aggregator.record(record(event = EventKey("application.metric_a"), timestamp = 0))
        aggregator.record(record(event = EventKey("application.metric_b"), timestamp = 1))
        aggregator.record(record(event = EventKey("application.metric_c"), timestamp = 2))
        assertEquals(EventKey("application.metric_a"), aggregator.snapshots().single().event)

        aggregator.record(record(event = EventKey("application.metric_b"), timestamp = 8))
        assertTrue(aggregator.snapshots().any { it.event == EventKey("application.metric_b") })
        assertTrue(aggregator.snapshots().size <= 2)
    }

    @Test
    fun `errors fatal and autofill delivery bypass debug sampling`() {
        val aggregator = EventAggregator(capacity = 8, debugSamplePercent = 0)
        assertFalse(aggregator.record(record(level = LogLevel.DEBUG)))
        assertTrue(aggregator.record(record(level = LogLevel.ERROR)))
        assertTrue(aggregator.record(record(level = LogLevel.FATAL)))
        assertTrue(
            aggregator.record(
                record(level = LogLevel.DEBUG, event = Events.Autofill.ResultDelivered),
            ),
        )
    }

    @Test
    fun `snapshot callback cannot recursively aggregate logger self metrics`() {
        lateinit var aggregator: EventAggregator
        var callbacks = 0
        aggregator = EventAggregator(capacity = 1, debugSamplePercent = 100) {
            callbacks++
            aggregator.record(record(event = EventKey("application.logger_metric")))
        }
        aggregator.record(record(event = EventKey("application.first_metric")))
        aggregator.record(record(event = EventKey("application.second_metric")))
        assertEquals(1, callbacks)
        assertEquals(1, aggregator.activeBucketCount)
    }

    private fun record(
        level: LogLevel = LogLevel.DEBUG,
        event: EventKey = Events.Application.BackgroundLockSkipped,
        timestamp: Long = 10,
        trace: String = "trace_12345678",
        failure: Boolean = false,
    ) = SanitizedLogRecord(
        eventId = EventId("event_${timestamp}_value"),
        timestampMs = timestamp,
        elapsedRealtimeMs = timestamp,
        level = level,
        category = event.category(),
        event = event,
        processSessionId = ProcessSessionId("process_123456"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId(trace), SpanId("span_123456789"), null),
        fields = fields { durationMs(LogFieldKeys.ElapsedMs, 7) },
        error = if (failure) {
            ErrorPayload(
                ErrorCodes.DatabaseSessionOpenFailed,
                Reasons.SessionOpenFailed,
                null,
            )
        } else null,
    )
}
