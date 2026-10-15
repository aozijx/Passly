package com.aozijx.passly.app.observability

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportTarget
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface DiagnosticsExportStorageGateway {
    fun clearStaleExports()
    fun createExportFile(): File
    fun share(file: File): Result<Unit>
    fun scheduleDeletion(file: File)
    fun delete(file: File)
    fun openDocument(uri: String): OutputStream
    fun deleteDocument(uri: String)
}

@Singleton
class ObservabilityExportService @Inject internal constructor(
    private val store: LogRecordStore,
    private val manifestFactory: ExportManifestFactory,
    private val storage: DiagnosticsExportStorageGateway,
    private val permitVerifier: AuthorizationPermitVerifier,
    private val exportEncoder: DiagnosticsExportEncoder = DiagnosticsExportEncoder(),
) : DiagnosticsExportGateway {
    override suspend fun export(
        permit: AuthorizationPermit,
        target: DiagnosticsExportTarget,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!permitVerifier.consume(permit, EXPORT_SCOPE)) {
                return@withContext Result.failure(SecurityException("Diagnostics export authorization rejected"))
            }
            runCatching {
                store.flush(FLUSH_TIMEOUT_MS)
                readRecords()
                    .distinctBy { it.eventId }
                    .sortedWith(
                        compareBy<SanitizedLogRecord> { it.timestampMs }
                            .thenBy { it.eventId.value },
                    )
            }.fold(
                onSuccess = { records ->
                    when (target) {
                        DiagnosticsExportTarget.Share -> exportForSharing(records)
                        is DiagnosticsExportTarget.Document -> exportToDocument(target.uri, records)
                    }
                },
                onFailure = Result.Companion::failure,
            )
        }

    override suspend fun discard(target: DiagnosticsExportTarget) {
        if (target is DiagnosticsExportTarget.Document) {
            withContext(Dispatchers.IO) { storage.deleteDocument(target.uri) }
        }
    }

    private fun exportForSharing(records: List<SanitizedLogRecord>): Result<Unit> {
        var exportFile: File? = null
        return try {
            storage.clearStaleExports()
            exportFile = storage.createExportFile()
            writeExport(exportFile.outputStream(), records)
            storage.share(exportFile).fold(
                onSuccess = {
                    storage.scheduleDeletion(exportFile)
                    Result.success(Unit)
                },
                onFailure = { failure ->
                    storage.delete(exportFile)
                    Result.failure(failure)
                },
            )
        } catch (failure: Exception) {
            exportFile?.let(storage::delete)
            Result.failure(failure)
        }
    }

    private fun exportToDocument(
        uri: String,
        records: List<SanitizedLogRecord>,
    ): Result<Unit> = runCatching {
        writeExport(storage.openDocument(uri), records)
    }.onFailure {
        storage.deleteDocument(uri)
    }

    private fun writeExport(
        output: OutputStream,
        records: List<SanitizedLogRecord>,
    ) {
        exportEncoder.write(
            format = LogFormat.JSONL,
            output = output,
            manifest = manifestFactory.create(records.size),
            records = records,
        )
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

internal class AndroidDiagnosticsExportStorageGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DiagnosticsExportStorageGateway {
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
                type = LogFormat.JSONL.mimeType
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

    override fun openDocument(uri: String): OutputStream {
        val documentUri = Uri.parse(uri)
        require(documentUri.scheme == "content")
        return context.contentResolver.openOutputStream(documentUri, "rwt")
            ?: error("Unable to open diagnostics export destination")
    }

    override fun deleteDocument(uri: String) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(uri)) }
    }

    private companion object {
        const val EXPORT_PREFIX = "passly_diagnostics_"
        const val DELETE_DELAY_MS = 10 * 60 * 1000L
    }
}
