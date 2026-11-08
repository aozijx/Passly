package com.aozijx.passly.data.observability

import com.aozijx.passly.core.observability.EnumName
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.ExceptionSnapshot
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
import com.aozijx.passly.core.observability.SanitizedStackFrame
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import java.nio.charset.CharacterCodingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LogRecordCodecTest {
    @Test
    fun `persisted enum wire ids are explicit stable protocol values`() {
        assertEquals(
            mapOf(
                LogLevel.DEBUG to 0,
                LogLevel.INFO to 1,
                LogLevel.WARN to 2,
                LogLevel.ERROR to 3,
                LogLevel.FATAL to 4,
            ),
            LogLevel.entries.associateWith(LogLevel::wireId),
        )
        assertEquals(
            mapOf(
                LogCategory.APPLICATION to 0,
                LogCategory.DATABASE to 1,
                LogCategory.SECURITY to 2,
                LogCategory.AUTOFILL to 3,
                LogCategory.BACKUP to 4,
                LogCategory.VAULT to 5,
            ),
            LogCategory.entries.associateWith(LogCategory::wireId),
        )
        assertEquals(
            mapOf(
                ExceptionKind.DATABASE_IO to 0,
                ExceptionKind.CRYPTO to 1,
                ExceptionKind.AUTHENTICATION to 2,
                ExceptionKind.PLATFORM to 3,
                ExceptionKind.LIFECYCLE to 4,
                ExceptionKind.IO to 5,
                ExceptionKind.UNKNOWN to 6,
            ),
            ExceptionKind.entries.associateWith(ExceptionKind::wireId),
        )
        LogLevel.entries.forEach { assertEquals(it, LogLevel.fromWireId(it.wireId)) }
        LogCategory.entries.forEach { assertEquals(it, LogCategory.fromWireId(it.wireId)) }
        ExceptionKind.entries.forEach { assertEquals(it, ExceptionKind.fromWireId(it.wireId)) }
    }

    @Test
    fun `round trip preserves all safe values trace hierarchy and error snapshot`() {
        val record = sampleRecord()
        val encoded = LogRecordCodec.encode(record)

        assertEquals(record, LogRecordCodec.decode(encoded))
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

        val eventBytes =
            Events.Autofill.ResultDeliveryFailed.value
                .encodeToByteArray()
        val eventOffset = encoded.indexOf(eventBytes)
        val malformed = encoded.copyOf().also { it[eventOffset] = 0xC3.toByte() }
        assertThrows(CharacterCodingException::class.java) {
            LogRecordCodec.decode(malformed)
        }
    }

    private fun sampleRecord() =
        SanitizedLogRecord(
            eventId = EventId("event_12345678"),
            timestampMs = 1_700_000_000_000,
            elapsedRealtimeMs = 42_000,
            level = LogLevel.ERROR,
            category = LogCategory.AUTOFILL,
            event = Events.Autofill.ResultDeliveryFailed,
            processSessionId = ProcessSessionId("process_12345678"),
            secureSessionId = SecureSessionId("secure_12345678"),
            trace =
                TraceSnapshot(
                    traceId = TraceId("trace_12345678"),
                    spanId = SpanId("span_12345678"),
                    parentSpanId = SpanId("parent_12345678"),
                ),
            fields =
                LogFields.fromSanitized(
                    linkedMapOf(
                        LogFieldKey("count") to LogValue.Count(2),
                        LogFieldKey("duration") to LogValue.DurationMs(120),
                        LogFieldKey("ratio") to LogValue.Ratio(0.5),
                        LogFieldKey("boolean") to LogValue.BooleanValue(true),
                        LogFieldKey("enum") to LogValue.EnumValue(EnumName("SUCCESS")),
                        LogFieldKey("error_code") to LogValue.ErrorCodeValue(ErrorCode("TEST_ERROR")),
                        LogFieldKey("reason") to LogValue.ReasonCodeValue(ReasonCode("TEST_REASON")),
                        LogFieldKey("operation") to LogValue.OperationCodeValue(OperationCode("test_operation")),
                        LogFieldKey("identifier") to
                            LogValue.HashedIdentifierValue(
                                HashedIdentifier("h1_abcdefghijklmnop"),
                            ),
                    ),
                ),
            error =
                ErrorPayload(
                    errorCode = ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
                    reason = ReasonCode("PLATFORM_CALLBACK_FAILED"),
                    exception =
                        ExceptionSnapshot(
                            type = ExceptionType("IllegalStateException"),
                            stackFrames =
                                listOf(
                                    SanitizedStackFrame("autofill.Result#deliver(Result.kt:42)"),
                                ),
                            causeTypes = listOf(ExceptionType("RuntimeException")),
                            externalFrameCount = 3,
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
