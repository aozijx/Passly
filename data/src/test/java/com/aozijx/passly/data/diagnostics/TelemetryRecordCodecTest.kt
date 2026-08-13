package com.aozijx.passly.data.diagnostics

import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TelemetryRecordCodecTest {
    private val event = TelemetryEvent(
        level = EventLevel.ERROR,
        category = EventCategory.SECURITY,
        name = "authentication.failed",
        fields = mapOf(
            "attempts" to SafeLogValue.Count(2),
            "elapsed" to SafeLogValue.DurationMs(120),
            "method" to SafeLogValue.EnumName("BIOMETRIC"),
            "code" to SafeLogValue.ErrorCodeValue(ErrorCode("AUTH_FAILED")),
            "operation" to SafeLogValue.OperationCodeValue(OperationCode("auth_verify"))
        ),
        throwableType = "SecurityException",
        appStackFrames = listOf("com.aozijx.passly.Auth.verify"),
        correlationId = "00000000-0000-0000-0000-000000000001",
        timestampMs = 123L
    )

    @Test
    fun roundTripPreservesStructuredRecord() {
        assertEquals(event, TelemetryRecordCodec.decode(TelemetryRecordCodec.encode(event)))
    }

    @Test
    fun decoderRejectsTrailingData() {
        val encoded = TelemetryRecordCodec.encode(event)
        assertThrows(IllegalArgumentException::class.java) {
            TelemetryRecordCodec.decode(encoded + 0x01)
        }
    }

    @Test
    fun decoderRejectsMalformedUtf8() {
        val encoded = TelemetryRecordCodec.encode(event)
        encoded[12] = 0xC3.toByte()
        encoded[13] = 0x28

        assertThrows(Exception::class.java) {
            TelemetryRecordCodec.decode(encoded)
        }
    }

    @Test
    fun encoderRejectsOversizedTypedCode() {
        val oversized = event.copy(
            fields = mapOf("method" to SafeLogValue.EnumName("A".repeat(65)))
        )

        assertThrows(IllegalArgumentException::class.java) {
            TelemetryRecordCodec.encode(oversized)
        }
    }
}
