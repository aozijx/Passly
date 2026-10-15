package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.fields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatSinkTest {
    @Test
    fun `release sink performs zero log calls even for fatal record`() {
        val gateway = RecordingLogGateway(loggable = true)
        var formatCalls = 0
        val sink = LogcatSink(
            isDebugBuild = false,
            gateway = gateway,
            formatter = LogcatRecordFormatter { formatCalls += 1; "must-not-format" },
        )

        sink.emit(record(level = LogLevel.FATAL))

        assertEquals(0, gateway.loggableChecks)
        assertEquals(0, gateway.lines.size)
        assertEquals(0, formatCalls)
    }

    @Test
    fun `disabled tag skips formatting and log call`() {
        val gateway = RecordingLogGateway(loggable = false)
        var formatCalls = 0
        val sink = LogcatSink(
            isDebugBuild = true,
            gateway = gateway,
            formatter = LogcatRecordFormatter { formatCalls += 1; "must-not-format" },
        )

        sink.emit(record())

        assertEquals(1, gateway.loggableChecks)
        assertEquals(0, gateway.lines.size)
        assertEquals(0, formatCalls)
    }

    @Test
    fun `debug sink emits one bounded line and never receives Throwable`() {
        val gateway = RecordingLogGateway(loggable = true)
        val sink = LogcatSink(isDebugBuild = true, gateway = gateway)

        sink.emit(record(level = LogLevel.ERROR, withError = true))

        val line = gateway.lines.single().message
        assertTrue(line.length <= LogcatSink.MAX_SUMMARY_CHARS)
        assertFalse(line.contains('\n'))
        assertTrue(line.contains("AUTOFILL_RESULT_DELIVERY_FAILED"))
        assertTrue(line.contains("IllegalStateException"))
        assertFalse(line.contains("Throwable"))
    }

    @Test
    fun `oversized summary emits identifiers and truncated flag in one line`() {
        val gateway = RecordingLogGateway(loggable = true)
        val sink = LogcatSink(isDebugBuild = true, gateway = gateway)
        val oversizedFields = fields {
            repeat(40) { index ->
                operation(
                    LogFieldKey("safe_field_$index"),
                    OperationCode("operation_${index}_" + "x".repeat(90)),
                )
            }
        }

        sink.emit(record(fields = oversizedFields))

        val line = gateway.lines.single().message
        assertTrue(line.length <= LogcatSink.MAX_SUMMARY_CHARS)
        assertFalse(line.contains('\n'))
        assertEquals(
            "event_id=event_123 name=autofill.result.delivery_failed trace_id=trace_123 " +
                "span_id=span_1234 truncated=true",
            line,
        )
    }

    private fun record(
        level: LogLevel = LogLevel.INFO,
        withError: Boolean = false,
        fields: com.aozijx.passly.core.observability.LogFields =
            com.aozijx.passly.core.observability.LogFields.Empty,
    ) = SanitizedLogRecord(
        eventId = EventId("event_123"),
        timestampMs = 1_700_000_000_000,
        elapsedRealtimeMs = 123,
        level = level,
        category = LogCategory.AUTOFILL,
        event = Events.Autofill.ResultDeliveryFailed,
        processSessionId = ProcessSessionId("process_123"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId("trace_123"), SpanId("span_1234"), null),
        fields = fields,
        error = if (withError) {
            ErrorPayload(
                errorCode = ErrorCodes.AutofillResultDeliveryFailed,
                reason = Reasons.PlatformCallbackFailed,
                exception = ExceptionSnapshot(
                    type = ExceptionType("IllegalStateException"),
                    appStackFrames = emptyList(),
                    causeTypes = emptyList(),
                    foreignFrameCount = 2,
                ),
            )
        } else {
            null
        },
    )

    private class RecordingLogGateway(
        private val loggable: Boolean,
    ) : AndroidLogGateway {
        var loggableChecks = 0
        val lines = mutableListOf<Line>()

        override fun isLoggable(tag: String, priority: AndroidLogPriority): Boolean {
            loggableChecks += 1
            return loggable
        }

        override fun println(priority: AndroidLogPriority, tag: String, message: String) {
            lines += Line(priority, tag, message)
        }
    }

    private data class Line(
        val priority: AndroidLogPriority,
        val tag: String,
        val message: String,
    )
}
