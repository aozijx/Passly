package com.aozijx.passly.app.observability

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class LogRecordJsonEncoder @Inject constructor() {
    fun encode(record: SanitizedLogRecord): String = toJson(record).toString()

    fun toJson(record: SanitizedLogRecord): JsonObject = buildJsonObject {
        put("record_type", "event")
        put("schema_version", ExportManifestFactory.SCHEMA_VERSION)
        put("event_id", record.eventId.value)
        put("timestamp_ms", record.timestampMs)
        put("elapsed_realtime_ms", record.elapsedRealtimeMs)
        put("level", record.level.name)
        put("category", record.category.name)
        put("name", record.event.value)
        put("process_session_id", record.processSessionId.value)
        put("secure_session_id", record.secureSessionId?.let { JsonPrimitive(it.value) } ?: JsonNull)
        put("trace", buildJsonObject {
            put("trace_id", record.trace.traceId.value)
            put("span_id", record.trace.spanId.value)
            put("parent_span_id", record.trace.parentSpanId?.let { JsonPrimitive(it.value) } ?: JsonNull)
        })
        put(
            "fields",
            JsonObject(
                record.fields.values.toSortedMap(compareBy { it.value }).map { (key, value) ->
                    key.value to value.toJsonValue()
                }.toMap(),
            ),
        )
        record.error?.let { error ->
            put("error", buildJsonObject {
                put("error_code", error.errorCode.value)
                put("reason", error.reason.value)
                put("diag_id", error.diagnosticId.value)
                error.exception?.let { exception ->
                    put("exception_type", exception.type.value)
                    put("stack_trace", JsonArray(exception.appStackFrames.map { JsonPrimitive(it.value) }))
                    put("cause_types", JsonArray(exception.causeTypes.map { JsonPrimitive(it.value) }))
                    put("foreign_frame_count", exception.foreignFrameCount)
                }
            })
        }
    }

    private fun LogValue.toJsonValue(): JsonElement = when (this) {
        is LogValue.Count -> JsonPrimitive(value)
        is LogValue.DurationMs -> JsonPrimitive(value)
        is LogValue.Ratio -> JsonPrimitive(value)
        is LogValue.BooleanValue -> JsonPrimitive(value)
        is LogValue.EnumValue -> JsonPrimitive(value.value)
        is LogValue.ErrorCodeValue -> JsonPrimitive(value.value)
        is LogValue.ReasonCodeValue -> JsonPrimitive(value.value)
        is LogValue.OperationCodeValue -> JsonPrimitive(value.value)
        is LogValue.HashedIdentifierValue -> JsonPrimitive(value.value)
        is LogValue.HashedIdentifierInput -> error("Pending identifier reached export")
    }
}

internal interface DiagnosticsExportFileGateway {
    fun clearStaleExports()
    fun createExportFile(): File
    fun share(file: File): Result<Unit>
    fun scheduleDeletion(file: File)
    fun delete(file: File)
}

@Singleton
class ObservabilityExportService @Inject internal constructor(
    private val store: LogRecordStore,
    private val manifestFactory: ExportManifestFactory,
    private val files: DiagnosticsExportFileGateway,
    private val permitVerifier: AuthorizationPermitVerifier,
    private val recordEncoder: LogRecordJsonEncoder = LogRecordJsonEncoder(),
) : DiagnosticsExportGateway {
    override suspend fun exportAndShare(permit: AuthorizationPermit): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!permitVerifier.consume(permit, EXPORT_SCOPE)) {
                return@withContext Result.failure(SecurityException("Diagnostics export authorization rejected"))
            }
            var target: File? = null
            try {
                files.clearStaleExports()
                store.flush(FLUSH_TIMEOUT_MS)
                val records = readRecords()
                    .distinctBy { it.eventId }
                    .sortedWith(
                        compareBy<SanitizedLogRecord> { it.timestampMs }
                            .thenBy { it.eventId.value },
                    )
                val exportFile = files.createExportFile()
                target = exportFile
                exportFile.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.appendLine(manifestFactory.create(records.size).toString())
                    records.forEach { writer.appendLine(recordEncoder.encode(it)) }
                }
                files.share(exportFile).fold(
                    onSuccess = {
                        files.scheduleDeletion(exportFile)
                        Result.success(Unit)
                    },
                    onFailure = { failure ->
                        files.delete(exportFile)
                        Result.failure(failure)
                    },
                )
            } catch (failure: Exception) {
                target?.let(files::delete)
                Result.failure(failure)
            }
        }

    private fun readRecords(): List<SanitizedLogRecord> {
        val records = ArrayList<SanitizedLogRecord>(MAX_EXPORT_RECORDS)
        var cursor: LogRecordCursor? = null
        do {
            val page = store.readPage(cursor, minOf(PAGE_SIZE, MAX_EXPORT_RECORDS - records.size))
            records += page.records.take(MAX_EXPORT_RECORDS - records.size)
            cursor = page.nextCursor
        } while (cursor != null && records.size < MAX_EXPORT_RECORDS)
        return records
    }

    private companion object {
        const val PAGE_SIZE = 500
        const val MAX_EXPORT_RECORDS = 5_000
        const val FLUSH_TIMEOUT_MS = 300L
        val EXPORT_SCOPE = AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS)
    }
}

internal class AndroidDiagnosticsExportFileGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DiagnosticsExportFileGateway {
    override fun clearStaleExports() {
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(EXPORT_PREFIX) }
            ?.forEach(File::delete)
    }

    override fun createExportFile(): File = File(
        context.cacheDir,
        "$EXPORT_PREFIX${System.currentTimeMillis()}.jsonl",
    )

    override fun share(file: File): Result<Unit> = runCatching {
        require(file.isFile && file.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/x-ndjson"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Passly diagnostics", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun scheduleDeletion(file: File) {
        Handler(Looper.getMainLooper()).postDelayed({ file.delete() }, DELETE_DELAY_MS)
    }

    override fun delete(file: File) {
        file.delete()
    }

    private companion object {
        const val EXPORT_PREFIX = "passly_diagnostics_"
        const val DELETE_DELAY_MS = 10 * 60 * 1000L
    }
}
