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
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportArtifactSpec
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(store, files)

        assertTrue(service.export(TestPermit, shareRequest()).isSuccess)

        val lines = files.sharedText!!.lineSequence().filter(String::isNotBlank)
            .map { Json.parseToJsonElement(it).jsonObject }.toList()
        assertEquals("manifest", lines.first().getValue("type").jsonPrimitive.content)
        assertEquals(
            listOf("event_00000001", "event_00000002"),
            lines.drop(1).map { it.getValue("event_id").jsonPrimitive.content },
        )
        assertEquals(listOf("100", "200"), lines.drop(1).map { it.getValue("timestamp_ms").jsonPrimitive.content })
        assertTrue(lines.drop(1).all { "schema_version" !in it })
        assertTrue(files.deleteScheduled)
        assertFalse(files.target!!.exists())
    }

    @Test
    fun `manifest contains only generic device and app context once`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        service.export(TestPermit, shareRequest()).getOrThrow()

        val records = files.sharedText!!.lineSequence().filter(String::isNotBlank)
            .map { Json.parseToJsonElement(it).jsonObject }.toList()
        val manifest = records.single { it["type"]?.jsonPrimitive?.content == "manifest" }
        assertEquals("Passly", manifest.getValue("app_name").jsonPrimitive.content)
        assertEquals("Pixel", manifest.getValue("model").jsonPrimitive.content)
        assertEquals(1, records.count { it["type"]?.jsonPrimitive?.content == "manifest" })
        val exported = files.sharedText!!
        listOf(
            "android_id",
            "fingerprint",
            "serial",
            "package_name",
            "com.example.secret",
        ).forEach {
            assertFalse(exported.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `document export writes directly to selected uri without share cache`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        service.export(
            TestPermit,
            DiagnosticsExportRequest(
                ExportAction.EXPORT_LOCAL,
                LogFormat.JSONL,
                "content://documents/log.jsonl",
            ),
        ).getOrThrow()

        assertTrue(files.documentText!!.startsWith("{\"type\":\"manifest\""))
        assertEquals(null, files.target)
        assertEquals(0, files.clearCalls)
        assertFalse(files.documentDeleted)
    }

    @Test
    fun `every share format uses its declared extension and mime type`() = runTest {
        LogFormat.entries.forEach { format ->
            val files = RecordingExportStorageGateway(cacheDirectory)
            val service = service(PageStore(listOf(record("event_00000001", 100))), files)

            service.export(TestPermit, shareRequest(format)).getOrThrow()

            assertEquals(format.extension, files.artifactSpec?.extension)
            assertEquals(format.mimeType, files.artifactSpec?.mimeType)
            assertEquals(format.mimeType, files.sharedMimeType)
            assertTrue(files.target!!.name.endsWith(".${format.extension}"))
            assertTrue(files.sharedBytes!!.isNotEmpty())
        }
    }

    @Test
    fun `every local format writes directly to the selected document`() = runTest {
        LogFormat.entries.forEach { format ->
            val files = RecordingExportStorageGateway(cacheDirectory)
            val service = service(PageStore(listOf(record("event_00000001", 100))), files)

            service.export(
                TestPermit,
                DiagnosticsExportRequest(
                    ExportAction.EXPORT_LOCAL,
                    format,
                    "content://documents/log.${format.extension}",
                ),
            ).getOrThrow()

            assertTrue(files.documentBytes!!.isNotEmpty())
            assertEquals(null, files.target)
            assertEquals(0, files.clearCalls)
            assertFalse(files.documentDeleted)
        }
    }

    @Test
    fun `failed share deletes plaintext immediately`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory, shareFailure = true)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        assertTrue(service.export(TestPermit, shareRequest()).isFailure)

        assertFalse(files.target!!.exists())
        assertFalse(files.deleteScheduled)
    }

    @Test
    fun `invalid permit fails before plaintext file is created`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(
            store = PageStore(listOf(record("event_00000001", 100))),
            files = files,
            permitAccepted = false,
        )

        assertTrue(service.export(TestPermit, shareRequest()).isFailure)

        assertEquals(null, files.target)
        assertEquals(0, files.clearCalls)
    }

    private fun service(
        store: LogRecordStore,
        files: RecordingExportStorageGateway,
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
                    )
                },
                clock = ExportClock { 1_700_000_000_000 },
            ),
            storage = files,
            permitVerifier = object : AuthorizationPermitVerifier {
                override fun consume(
                    permit: AuthorizationPermit,
                    expectedScope: AuthorizationScope,
                ): Boolean = permitAccepted && expectedScope == AuthorizationScope.Global(
                    AuthenticationPurpose.EXPORT_DIAGNOSTICS,
                )
            },
        )

    private fun shareRequest(format: LogFormat = LogFormat.JSONL) =
        DiagnosticsExportRequest(ExportAction.SHARE, format)

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

    private class RecordingExportStorageGateway(
        private val directory: File,
        private val shareFailure: Boolean = false,
    ) : DiagnosticsExportStorageGateway {
        var target: File? = null
        var sharedText: String? = null
        var sharedBytes: ByteArray? = null
        var deleteScheduled = false
        var clearCalls = 0
        var documentText: String? = null
        var documentBytes: ByteArray? = null
        var documentDeleted = false
        var artifactSpec: ExportArtifactSpec? = null
        var sharedMimeType: String? = null

        override fun clearStaleExports() {
            clearCalls++
        }

        override fun createExportFile(spec: ExportArtifactSpec): File = File(
            directory,
            "passly_diagnostics.${spec.extension}",
        ).also {
            artifactSpec = spec
            target = it
        }

        override fun share(file: File, mimeType: String): Result<Unit> {
            sharedMimeType = mimeType
            sharedBytes = file.readBytes()
            sharedText = sharedBytes!!.toString(Charsets.UTF_8)
            return if (shareFailure) Result.failure(IllegalStateException("share failed")) else Result.success(Unit)
        }

        override fun scheduleDeletion(file: File) {
            deleteScheduled = true
            file.delete()
        }

        override fun delete(file: File) {
            file.delete()
        }

        override fun openDocument(uri: String): OutputStream = object : ByteArrayOutputStream() {
            override fun close() {
                documentBytes = toByteArray()
                documentText = documentBytes!!.toString(Charsets.UTF_8)
                super.close()
            }
        }

        override fun deleteDocument(uri: String) {
            documentDeleted = true
        }
    }

    private object TestPermit : AuthorizationPermit
}
