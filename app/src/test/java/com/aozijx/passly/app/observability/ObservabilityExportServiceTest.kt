package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordPage
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObservabilityExportServiceTest {
    private lateinit var cacheDirectory: File

    @Before
    fun setUp() {
        cacheDirectory = Files.createTempDirectory("passly-export").toFile()
    }

    @After
    fun tearDown() {
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun `first line is manifest and current records are ordered deduplicated and schema shaped`() = runTest {
        val duplicate = record("event_00000002", timestampMs = 200)
        val store = PageStore(
            listOf(
                duplicate,
                record("event_00000001", timestampMs = 100),
                duplicate,
            ),
        )
        val files = RecordingExportFileGateway(cacheDirectory)
        val service = service(store, files)

        assertTrue(service.exportAndShare(TestPermit).isSuccess)

        val lines = files.sharedText!!.lineSequence().filter(String::isNotBlank).map(Json::parseToJsonElement).toList()
        assertEquals("manifest", lines.first().jsonObject.getValue("record_type").jsonPrimitive.content)
        assertEquals(
            listOf("event_00000001", "event_00000002"),
            lines.drop(1).map { it.jsonObject.getValue("event_id").jsonPrimitive.content },
        )
        lines.forEach(::assertSchemaShape)
        assertTrue(files.deleteScheduled)
        assertFalse(files.target!!.exists())
    }

    @Test
    fun `manifest contains device app and autofill context once without forbidden identifiers`() = runTest {
        val files = RecordingExportFileGateway(cacheDirectory)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        service.exportAndShare(TestPermit).getOrThrow()

        val records = files.sharedText!!.lineSequence().filter(String::isNotBlank)
            .map(Json::parseToJsonElement).map(JsonElement::jsonObject).toList()
        val manifest = records.single { it["record_type"]?.jsonPrimitive?.content == "manifest" }
        assertEquals("Passly", manifest.getValue("app").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("Pixel", manifest.getValue("device").jsonObject.getValue("model").jsonPrimitive.content)
        assertEquals(
            true,
            manifest.getValue("autofill").jsonObject.getValue("legacy_service_enabled").jsonPrimitive.content.toBoolean(),
        )
        assertEquals(1, records.count { "device" in it })
        val exported = files.sharedText!!
        listOf("android_id", "fingerprint", "serial", "package_name", "com.example.secret").forEach {
            assertFalse(exported.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `schema resource is stable named and describes every emitted record type`() {
        val schemaText = checkNotNull(javaClass.classLoader!!.getResource("observability.schema.json"))
            .readText()
        val schema = Json.parseToJsonElement(schemaText).jsonObject

        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.getValue("\$schema").jsonPrimitive.content)
        val definitions = schema.getValue("\$defs").jsonObject
        assertTrue(setOf("manifest", "event", "trace", "error", "safeValue").all(definitions::containsKey))
    }

    @Test
    fun `failed share deletes plaintext immediately`() = runTest {
        val files = RecordingExportFileGateway(cacheDirectory, shareFailure = true)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        assertTrue(service.exportAndShare(TestPermit).isFailure)

        assertFalse(files.target!!.exists())
        assertFalse(files.deleteScheduled)
    }

    @Test
    fun `invalid permit fails before plaintext file is created`() = runTest {
        val files = RecordingExportFileGateway(cacheDirectory)
        val service = service(
            store = PageStore(listOf(record("event_00000001", 100))),
            files = files,
            permitAccepted = false,
        )

        assertTrue(service.exportAndShare(TestPermit).isFailure)

        assertEquals(null, files.target)
        assertEquals(0, files.clearCalls)
    }

    private fun service(
        store: LogRecordStore,
        files: RecordingExportFileGateway,
        permitAccepted: Boolean = true,
    ) =
        ObservabilityExportService(
            store = store,
            manifestFactory = ExportManifestFactory(
                contextProvider = ExportContextProvider {
                    ExportContext(
                        appName = "Passly",
                        appVersionName = "0.3.3-debug",
                        appVersionCode = 8,
                        manufacturer = "Google",
                        model = "Pixel",
                        sdkInt = 36,
                        osRelease = "16",
                        locales = listOf("zh-CN"),
                        timezone = "Asia/Shanghai",
                        legacyAutofillEnabled = true,
                        credentialProviderAvailable = true,
                        inlineSuggestionsSupported = true,
                    )
                },
                clock = ExportClock { 1_700_000_000_000 },
            ),
            files = files,
            permitVerifier = object : AuthorizationPermitVerifier {
                override fun consume(
                    permit: AuthorizationPermit,
                    expectedScope: AuthorizationScope,
                ): Boolean = permitAccepted && expectedScope == AuthorizationScope.Global(
                    AuthenticationPurpose.EXPORT_DIAGNOSTICS,
                )
            },
        )

    private fun assertSchemaShape(element: JsonElement) {
        val value = element.jsonObject
        assertEquals(1, value.getValue("schema_version").jsonPrimitive.content.toInt())
        when (value.getValue("record_type").jsonPrimitive.content) {
            "manifest" -> {
                assertTrue(setOf("export", "app", "device", "autofill").all(value::containsKey))
            }
            "event" -> {
                val required = setOf(
                    "event_id", "timestamp_ms", "elapsed_realtime_ms", "level", "category",
                    "name", "process_session_id", "trace", "fields",
                )
                assertTrue(required.all(value::containsKey))
                assertTrue(setOf("trace_id", "span_id").all(value.getValue("trace").jsonObject::containsKey))
            }
            else -> throw AssertionError("Unexpected record type")
        }
    }

    private fun record(eventId: String, timestampMs: Long) = SanitizedLogRecord(
        eventId = EventId(eventId),
        timestampMs = timestampMs,
        elapsedRealtimeMs = timestampMs,
        level = LogLevel.INFO,
        category = LogCategory.AUTOFILL,
        event = Events.Autofill.ResultDelivered,
        processSessionId = ProcessSessionId("process_12345678"),
        secureSessionId = null,
        trace = TraceSnapshot(
            TraceId("trace_12345678"),
            SpanId("span_${eventId.takeLast(8)}"),
            null,
        ),
        fields = LogFields.Empty,
        error = null,
    )

    private class PageStore(private val records: List<SanitizedLogRecord>) : LogRecordStore {
        override fun write(record: SanitizedLogRecord) = Unit
        override fun readPage(cursor: LogRecordCursor?, limit: Int): LogRecordPage =
            LogRecordPage(records.take(limit), records.size, null)
        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) =
            records.filter { it.timestampMs in startTimestampMs until endTimestampMs }.take(limit)
        override fun flush(timeoutMs: Long) = true
        override fun clear() = Unit
        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long) = true
        override fun close() = Unit
    }

    private class RecordingExportFileGateway(
        private val directory: File,
        private val shareFailure: Boolean = false,
    ) : DiagnosticsExportFileGateway {
        var target: File? = null
        var sharedText: String? = null
        var deleteScheduled = false
        var clearCalls = 0

        override fun clearStaleExports() {
            clearCalls++
        }

        override fun createExportFile(): File = File(directory, "passly_diagnostics.jsonl").also {
            target = it
        }

        override fun share(file: File): Result<Unit> {
            sharedText = file.readText()
            return if (shareFailure) Result.failure(IllegalStateException("share failed")) else Result.success(Unit)
        }

        override fun scheduleDeletion(file: File) {
            deleteScheduled = true
            file.delete()
        }

        override fun delete(file: File) {
            file.delete()
        }
    }

    private object TestPermit : AuthorizationPermit
}
