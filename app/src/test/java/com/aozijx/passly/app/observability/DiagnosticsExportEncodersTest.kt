package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.AppStackFrame
import com.aozijx.passly.core.observability.EnumName
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsExportEncodersTest {
    private val manifest = ExportManifest(
        schemaVersion = 1,
        generatedAtMs = 1_700_000_000_999,
        recordCount = 2,
        appName = "Passly",
        appVersionName = "0.3.3",
        appVersionCode = 8,
        manufacturer = "Google",
        model = "Pixel",
        sdkInt = 35,
        osRelease = "15",
        locales = listOf("zh-CN", "en-US"),
        timezone = "Asia/Shanghai",
    )
    private val encoder = DiagnosticsExportEncoder()

    @Test
    fun `standalone jsonl has one manifest then stable canonical events`() {
        val text = encode(LogFormat.JSONL, listOf(record("event_00000002", 100, 12), record("event_00000001", 100, 11)))
        val objects = text.lineSequence().filter(String::isNotBlank).map { Json.parseToJsonElement(it).jsonObject }.toList()

        assertEquals("manifest", objects.first().getValue("type").jsonPrimitive.content)
        assertEquals(listOf("event_00000001", "event_00000002"), objects.drop(1).map { it.getValue("event_id").jsonPrimitive.content })
        assertEquals(
            listOf("time", "timestamp_ms", "elapsed_realtime_ms", "level", "event", "event_id", "context", "error", "trace", "session"),
            objects.last().keys.toList(),
        )
        assertFalse(objects.last().containsKey("schema_version"))
    }

    @Test
    fun `standalone tsv has manifest header and fixed time first eight columns`() {
        val lines = encode(LogFormat.TSV, listOf(record("event_00000001", 100, 11))).lines()

        assertTrue(lines[0].startsWith("# schema_version=1"))
        assertEquals("time\tlevel\tevent\toutcome\tdetails\texception\ttrace\tsession", lines[1])
        assertEquals(8, lines[2].split('\t').size)
        assertTrue(lines[2].startsWith("1970-01-01T00:00:00.100Z\tERROR\t"))
    }

    @Test
    fun `tsv escapes reserved characters without changing column shape`() {
        val event = CanonicalDiagnosticsEvent.from(record("event_00000001", 100, 11)).toJson().toMutableMap().let {
            JsonObject(it + ("future" to buildJsonObject { put("value", JsonPrimitive("a\tb\nc\\d;e=f")) }))
        }
        val row = JsonEventToTsvProjector().project(event)

        assertEquals(8, row.split('\t').size)
        assertTrue(row.contains("future.value=a\\tb\\nc\\\\d\\;e\\=f"))
    }

    @Test
    fun `zip contains manifest and two manifest free projections plus readme`() {
        val bytes = encodeBytes(LogFormat.ZIP, listOf(record("event_00000001", 100, 11)))
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().decodeToString()
            }
        }

        assertEquals(listOf("manifest.json", "logs.jsonl", "logs.tsv", "README.txt"), entries.keys.toList())
        assertFalse(entries.getValue("logs.jsonl").contains("\"type\":\"manifest\""))
        assertFalse(entries.getValue("logs.tsv").startsWith("#"))
    }

    @Test
    fun `safe exception metadata survives and contaminating messages never appear`() {
        val secret = "P@ssw0rd! com.example.secret C:\\private\\vault token=abc"
        val record = record("event_00000001", 100, 11).copy(
            error = ErrorPayload(
                ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
                ReasonCode("PLATFORM_CALLBACK_FAILED"),
                ExceptionSnapshot(
                    ExceptionType("IllegalStateException"),
                    listOf(AppStackFrame("com.aozijx.passly.Safe#call:42")),
                    emptyList(),
                    1,
                    ExceptionKind.PLATFORM,
                ),
            ),
        )

        assertTrue(encodeBytes(LogFormat.JSONL, listOf(record)).decodeToString().contains("PLATFORM"))
        LogFormat.entries.forEach { format ->
            val exported = encodeBytes(format, listOf(record)).decodeToString()
            assertFalse(exported.contains(secret))
            assertFalse(exported.contains("com.example.secret"))
        }
    }

    private fun encode(format: LogFormat, records: List<SanitizedLogRecord>): String =
        encodeBytes(format, records).decodeToString()

    private fun encodeBytes(format: LogFormat, records: List<SanitizedLogRecord>): ByteArray =
        ByteArrayOutputStream().also { encoder.write(format, it, manifest.copy(recordCount = records.size), records) }.toByteArray()

    private fun record(eventId: String, timestamp: Long, elapsed: Long) = SanitizedLogRecord(
        eventId = EventId(eventId),
        timestampMs = timestamp,
        elapsedRealtimeMs = elapsed,
        level = LogLevel.ERROR,
        category = LogCategory.AUTOFILL,
        event = Events.Autofill.ResultDeliveryFailed,
        processSessionId = ProcessSessionId("process_12345678"),
        secureSessionId = SecureSessionId("secure_12345678"),
        trace = TraceSnapshot(TraceId("trace_12345678"), SpanId("span_12345678"), null),
        fields = LogFields.fromSanitized(
            linkedMapOf(
                LogFieldKey("result") to LogValue.EnumValue(EnumName("FAILED")),
                LogFieldKey("retry_count") to LogValue.Count(2),
            ),
        ),
        error = ErrorPayload(
            ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
            ReasonCode("PLATFORM_CALLBACK_FAILED"),
            ExceptionSnapshot(ExceptionType("IllegalStateException"), emptyList(), emptyList(), 0),
        ),
    )
}
