package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
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
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportTarget
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
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

        assertTrue(service.export(TestPermit, DiagnosticsExportTarget.Share).isSuccess)

        val lines = files.sharedText!!.lineSequence().filter(String::isNotBlank)
            .map { it.split('\t') }.toList()
        assertEquals("manifest", lines.first()[0])
        assertEquals(
            listOf("event_00000001", "event_00000002"),
            lines.drop(1).map { it[2] },
        )
        assertEquals(listOf("100", "200"), lines.drop(1).map { it[3] })
        assertTrue(lines.drop(1).all { it.size == 20 })
        assertFalse(files.sharedText!!.contains("timestamp_ms"))
        assertTrue(files.deleteScheduled)
        assertFalse(files.target!!.exists())
    }

    @Test
    fun `manifest contains only generic device and app context once`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        service.export(TestPermit, DiagnosticsExportTarget.Share).getOrThrow()

        val records = files.sharedText!!.lineSequence().filter(String::isNotBlank)
            .map { it.split('\t') }.toList()
        val manifest = records.single { it[0] == "manifest" }
        assertEquals(13, manifest.size)
        assertEquals("Passly", manifest[4])
        assertEquals("Pixel", manifest[8])
        assertEquals(1, records.count { it[0] == "manifest" })
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
    fun `text row keeps fixed value positions and omits protocol key names`() {
        val encoded = LogRecordTextEncoder().encode(
            record("event_00000003", 300).copy(
                level = LogLevel.ERROR,
                fields = LogFields.fromSanitized(
                    mapOf(LogFieldKey("retry_count") to LogValue.Count(2)),
                ),
                error = ErrorPayload(
                    errorCode = ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
                    reason = ReasonCode("PLATFORM_CALLBACK_FAILED"),
                    exception = ExceptionSnapshot(
                        type = ExceptionType("IllegalStateException"),
                        appStackFrames = emptyList(),
                        causeTypes = emptyList(),
                        foreignFrameCount = 4,
                    ),
                ),
            ),
        )

        val columns = encoded.split('\t')
        assertEquals(20, columns.size)
        assertEquals("300", columns[3])
        assertEquals("2", columns[13])
        assertEquals("AUTOFILL_RESULT_DELIVERY_FAILED", columns[14])
        assertEquals("PLATFORM_CALLBACK_FAILED", columns[15])
        assertEquals("IllegalStateException", columns[16])
        assertEquals("4", columns[19])
        assertFalse(encoded.contains("timestamp_ms"))
    }

    @Test
    fun `document export writes directly to selected uri without share cache`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        service.export(
            TestPermit,
            DiagnosticsExportTarget.Document("content://documents/log.txt"),
        ).getOrThrow()

        assertTrue(files.documentText!!.startsWith("manifest\t"))
        assertEquals(null, files.target)
        assertEquals(0, files.clearCalls)
        assertFalse(files.documentDeleted)
    }

    @Test
    fun `failed share deletes plaintext immediately`() = runTest {
        val files = RecordingExportStorageGateway(cacheDirectory, shareFailure = true)
        val service = service(PageStore(listOf(record("event_00000001", 100))), files)

        assertTrue(service.export(TestPermit, DiagnosticsExportTarget.Share).isFailure)

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

        assertTrue(service.export(TestPermit, DiagnosticsExportTarget.Share).isFailure)

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
        var deleteScheduled = false
        var clearCalls = 0
        var documentText: String? = null
        var documentDeleted = false

        override fun clearStaleExports() {
            clearCalls++
        }

        override fun createExportFile(): File = File(directory, "passly_diagnostics.txt").also {
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

        override fun openDocument(uri: String): OutputStream = object : ByteArrayOutputStream() {
            override fun close() {
                documentText = toString(Charsets.UTF_8.name())
                super.close()
            }
        }

        override fun deleteDocument(uri: String) {
            documentDeleted = true
        }
    }

    private object TestPermit : AuthorizationPermit
}
