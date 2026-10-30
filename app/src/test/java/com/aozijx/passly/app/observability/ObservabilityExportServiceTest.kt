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
import com.aozijx.passly.core.observability.sink.LogDropCounts
import com.aozijx.passly.core.observability.sink.LogClearResult
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
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
    fun `failed deletion scheduling keeps an already shared export available`() = runTest {
        val files = RecordingExportStorageGateway(
            cacheDirectory,
            scheduleDeletionAccepted = false,
        )
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        val result = service.export(TestPermit, shareRequest())

        assertTrue(result.isSuccess)
        assertEquals(1, files.shareCalls)
        assertFalse(files.deleteScheduled)
        assertTrue(files.target!!.exists())
    }

    @Test
    fun `deletion scheduler exception keeps an already shared export available`() = runTest {
        val files = RecordingExportStorageGateway(
            cacheDirectory,
            scheduleDeletionFailure = IllegalStateException("handler unavailable"),
        )
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        val result = service.export(TestPermit, shareRequest())

        assertTrue(result.isSuccess)
        assertEquals(1, files.shareCalls)
        assertFalse(files.deleteScheduled)
        assertTrue(files.target!!.exists())
    }

    @Test
    fun `cancellation while reading records propagates`() = runTest {
        val cancelled = CancellationException("diagnostics read cancelled")
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(emptyList(), readFailure = cancelled), files)

        val thrown = try {
            service.export(TestPermit, shareRequest())
            null
        } catch (failure: CancellationException) {
            failure
        }

        assertEquals(cancelled.message, thrown?.message)
        assertEquals(null, files.target)
    }

    @Test
    fun `coroutine cancellation during record flush stops before export`() = runTest {
        var exportJob: Job? = null
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(emptyList(), onFlush = { exportJob?.cancel() }), files)
        val job = launch {
            exportJob = currentCoroutineContext()[Job]
            service.export(TestPermit, shareRequest())
        }

        job.join()

        assertTrue(job.isCancelled)
        assertEquals(null, files.target)
        assertEquals(0, files.shareCalls)
    }

    @Test
    fun `cancellation from share deletes plaintext then propagates`() = runTest {
        val cancelled = CancellationException("diagnostics share cancelled")
        val files = RecordingExportStorageGateway(cacheDirectory, shareException = cancelled)
        val service = service(PageStore(emptyList()), files)

        val thrown = try {
            service.export(TestPermit, shareRequest())
            null
        } catch (failure: CancellationException) {
            failure
        }

        assertEquals(cancelled.message, thrown?.message)
        assertNotNull(files.target)
        assertFalse(files.target!!.exists())
        assertFalse(files.deleteScheduled)
    }

    @Test
    fun `coroutine cancellation after share file creation prevents sharing and deletes file`() = runTest {
        var exportJob: Job? = null
        val files = RecordingExportStorageGateway(cacheDirectory, afterCreateFile = { exportJob?.cancel() })
        val service = service(PageStore(emptyList()), files)
        val job = launch {
            exportJob = currentCoroutineContext()[Job]
            service.export(TestPermit, shareRequest())
        }

        job.join()

        assertTrue(job.isCancelled)
        assertNotNull(files.target)
        assertFalse(files.target!!.exists())
        assertEquals(0, files.shareCalls)
    }

    @Test
    fun `cancellation while writing document deletes target then propagates`() = runTest {
        val cancelled = CancellationException("diagnostics document write cancelled")
        val files = RecordingExportStorageGateway(cacheDirectory, documentWriteException = cancelled)
        val service = service(PageStore(emptyList()), files)

        val thrown = try {
            service.export(
                TestPermit,
                DiagnosticsExportRequest(ExportAction.EXPORT_LOCAL, LogFormat.JSONL, "content://documents/log.jsonl"),
            )
            null
        } catch (failure: CancellationException) {
            failure
        }

        assertEquals(cancelled.message, thrown?.message)
        assertTrue(files.documentDeleted)
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

    private class PageStore(
        private val records: List<SanitizedLogRecord>,
        private val readFailure: Throwable? = null,
        private val onFlush: (() -> Unit)? = null,
    ) : LogRecordStore {
        override fun write(record: SanitizedLogRecord) = Unit
        override fun readPage(cursor: LogRecordCursor?, limit: Int): LogRecordPage {
            readFailure?.let { throw it }
            return LogRecordPage(records.take(limit), records.size, null)
        }
        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) =
            records.filter { it.timestampMs in startTimestampMs until endTimestampMs }.take(limit)
        override fun storageBytes() = 0L
        override fun drainDroppedCounts() = LogDropCounts()
        override fun flush(timeoutMs: Long): Boolean {
            onFlush?.invoke()
            return true
        }
        override fun clear() = LogClearResult.Cleared
        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long) = true
        override fun close() = Unit
    }

    private class RecordingExportStorageGateway(
        private val directory: File,
        private val shareFailure: Boolean = false,
        private val shareException: Throwable? = null,
        private val scheduleDeletionFailure: Throwable? = null,
        private val scheduleDeletionAccepted: Boolean = true,
        private val documentWriteException: Throwable? = null,
        private val afterCreateFile: (() -> Unit)? = null,
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
        var shareCalls = 0

        override fun clearStaleExports() {
            clearCalls++
        }

        override fun createExportFile(spec: ExportArtifactSpec): File = File(
            directory,
            "passly_logs.${spec.extension}",
        ).also {
            artifactSpec = spec
            target = it
            afterCreateFile?.invoke()
        }

        override fun share(file: File, mimeType: String): Result<Unit> {
            shareCalls++
            shareException?.let { throw it }
            sharedMimeType = mimeType
            sharedBytes = file.readBytes()
            sharedText = sharedBytes!!.toString(Charsets.UTF_8)
            return if (shareFailure) Result.failure(IllegalStateException("share failed")) else Result.success(Unit)
        }

        override fun scheduleDeletion(file: File): Boolean {
            scheduleDeletionFailure?.let { throw it }
            deleteScheduled = scheduleDeletionAccepted
            if (scheduleDeletionAccepted) file.delete()
            return scheduleDeletionAccepted
        }

        override fun delete(file: File) {
            file.delete()
        }

        override fun openDocument(uri: String): OutputStream = object : ByteArrayOutputStream() {
            override fun write(value: Int) {
                documentWriteException?.let { throw it }
                super.write(value)
            }

            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                documentWriteException?.let { throw it }
                super.write(bytes, offset, length)
            }

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
