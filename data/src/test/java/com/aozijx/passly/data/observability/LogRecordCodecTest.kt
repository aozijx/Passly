package com.aozijx.passly.data.observability

import com.aozijx.passly.core.observability.AppStackFrame
import com.aozijx.passly.core.observability.EnumName
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LogRecordCodecTest {
    @Test
    fun `round trip preserves all safe values trace hierarchy and error snapshot`() {
        val record = sampleRecord()
        val encoded = LogRecordCodec.encode(record)

        assertEquals(record, LogRecordCodec.decode(encoded))
        assertFalse(encoded.decodeToString().contains("DGN-"))
    }

    @Test
    fun `decoder rejects trailing bytes invalid enum and malformed utf8`() {
        val encoded = LogRecordCodec.encode(sampleRecord())
        assertThrows(IllegalArgumentException::class.java) {
            LogRecordCodec.decode(encoded + 1)
        }

        val badLevel = encoded.copyOf().also { it[20] = 127 }
        assertThrows(IllegalArgumentException::class.java) {
            LogRecordCodec.decode(badLevel)
        }

        val eventBytes = Events.Autofill.ResultDeliveryFailed.value.encodeToByteArray()
        val eventOffset = encoded.indexOf(eventBytes)
        val malformed = encoded.copyOf().also { it[eventOffset] = 0xC3.toByte() }
        assertThrows(Exception::class.java) {
            LogRecordCodec.decode(malformed)
        }
    }

    @Test
    fun `sanitized record boundary refuses pending unhashed identifiers`() {
        assertThrows(IllegalArgumentException::class.java) {
            LogFields.fromSanitized(
                mapOf(
                    LogFieldKey("target_package") to LogValue.HashedIdentifierInput(
                        com.aozijx.passly.core.observability.IdentifierNamespace.TARGET_PACKAGE,
                        "com.example.secret",
                    ),
                ),
            )
        }
    }

    private fun sampleRecord() = SanitizedLogRecord(
        eventId = EventId("event_12345678"),
        timestampMs = 1_700_000_000_000,
        elapsedRealtimeMs = 42_000,
        level = LogLevel.ERROR,
        category = LogCategory.AUTOFILL,
        event = Events.Autofill.ResultDeliveryFailed,
        processSessionId = ProcessSessionId("process_12345678"),
        secureSessionId = SecureSessionId("secure_12345678"),
        trace = TraceSnapshot(
            traceId = TraceId("trace_12345678"),
            spanId = SpanId("span_12345678"),
            parentSpanId = SpanId("parent_12345678"),
        ),
        fields = LogFields.fromSanitized(
            linkedMapOf(
                LogFieldKey("count") to LogValue.Count(2),
                LogFieldKey("duration") to LogValue.DurationMs(120),
                LogFieldKey("ratio") to LogValue.Ratio(0.5),
                LogFieldKey("boolean") to LogValue.BooleanValue(true),
                LogFieldKey("enum") to LogValue.EnumValue(EnumName("SUCCESS")),
                LogFieldKey("error_code") to LogValue.ErrorCodeValue(ErrorCode("TEST_ERROR")),
                LogFieldKey("reason") to LogValue.ReasonCodeValue(ReasonCode("TEST_REASON")),
                LogFieldKey("operation") to LogValue.OperationCodeValue(OperationCode("test_operation")),
                LogFieldKey("identifier") to LogValue.HashedIdentifierValue(
                    HashedIdentifier("h1_abcdefghijklmnop"),
                ),
            ),
        ),
        error = ErrorPayload(
            errorCode = ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
            reason = ReasonCode("PLATFORM_CALLBACK_FAILED"),
            exception = ExceptionSnapshot(
                type = ExceptionType("IllegalStateException"),
                appStackFrames = listOf(
                    AppStackFrame("com.aozijx.passly.autofill.Result#deliver:42"),
                ),
                causeTypes = listOf(ExceptionType("RuntimeException")),
                foreignFrameCount = 3,
                kind = ExceptionKind.PLATFORM,
            ),
        ),
    )

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        for (start in 0..size - needle.size) {
            if (needle.indices.all { this[start + it] == needle[it] }) return start
        }
        error("needle not found")
    }
}
