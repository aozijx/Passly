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
internal class LogRecordTextEncoder @Inject constructor() {
    fun encode(record: SanitizedLogRecord): String = values(
        "event",
        ExportManifestFactory.SCHEMA_VERSION,
        record.eventId.value,
        record.timestampMs,
        record.elapsedRealtimeMs,
        record.level.name,
        record.category.name,
        record.event.value,
        record.processSessionId.value,
        record.secureSessionId?.value,
        record.trace.traceId.value,
        record.trace.spanId.value,
        record.trace.parentSpanId?.value,
        record.fields.values.toSortedMap(compareBy { it.value }).values.joinToString(";") { it.asText() },
        record.error?.errorCode?.value,
        record.error?.reason?.value,
        record.error?.diagnosticId?.value,
        record.error?.exception?.type?.value,
        record.error?.exception?.appStackFrames?.joinToString(";") { it.value },
        record.error?.exception?.causeTypes?.joinToString(";") { it.value },
        record.error?.exception?.foreignFrameCount,
    )

    private fun LogValue.asText(): String = when (this) {
        is LogValue.Count -> value.toString()
        is LogValue.DurationMs -> value.toString()
        is LogValue.Ratio -> value.toString()
        is LogValue.BooleanValue -> value.toString()
        is LogValue.EnumValue -> value.value
        is LogValue.ErrorCodeValue -> value.value
        is LogValue.ReasonCodeValue -> value.value
        is LogValue.OperationCodeValue -> value.value
        is LogValue.HashedIdentifierValue -> value.value
        is LogValue.HashedIdentifierInput -> error("Pending identifier reached export")
    }
}

internal fun values(vararg values: Any?): String = values.joinToString("\t") {
    it?.toString().orEmpty()
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
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
    private val recordEncoder: LogRecordTextEncoder = LogRecordTextEncoder(),
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
                    writer.appendLine(manifestFactory.encode(records.size))
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
        "$EXPORT_PREFIX${System.currentTimeMillis()}.txt",
    )

    override fun share(file: File): Result<Unit> = runCatching {
        require(file.isFile && file.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
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
