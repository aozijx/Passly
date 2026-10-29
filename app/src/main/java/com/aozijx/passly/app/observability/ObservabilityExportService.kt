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
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportArtifactSpec
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.feature.settings.diagnostics.artifactSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal interface DiagnosticsExportStorageGateway {
    fun clearStaleExports()
    fun createExportFile(spec: ExportArtifactSpec): File
    fun share(file: File, mimeType: String): Result<Unit>
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
        request: DiagnosticsExportRequest,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            if (!permitVerifier.consume(permit, EXPORT_SCOPE)) {
                return@withContext Result.failure(SecurityException("Diagnostics export authorization rejected"))
            }
            currentCoroutineContext().ensureActive()
            val records = try {
                store.flush(FLUSH_TIMEOUT_MS)
                currentCoroutineContext().ensureActive()
                readRecords()
                    .distinctBy { it.eventId }
                    .sortedWith(
                        compareBy<SanitizedLogRecord> { it.timestampMs }
                            .thenBy { it.elapsedRealtimeMs }
                            .thenBy { it.eventId.value },
                    )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                return@withContext Result.failure(failure)
            }
            currentCoroutineContext().ensureActive()
            when (request.action) {
                ExportAction.SHARE -> exportForSharing(records, request.format)
                ExportAction.EXPORT_LOCAL -> exportToDocument(
                    requireNotNull(request.documentUri),
                    records,
                    request.format,
                )
            }
        }

    override suspend fun discard(request: DiagnosticsExportRequest) {
        if (request.action == ExportAction.EXPORT_LOCAL) {
            withContext(Dispatchers.IO) { storage.deleteDocument(requireNotNull(request.documentUri)) }
        }
    }

    private suspend fun exportForSharing(records: List<SanitizedLogRecord>, format: LogFormat): Result<Unit> {
        var exportFile: File? = null
        return try {
            currentCoroutineContext().ensureActive()
            storage.clearStaleExports()
            currentCoroutineContext().ensureActive()
            val spec = format.artifactSpec()
            exportFile = storage.createExportFile(spec)
            currentCoroutineContext().ensureActive()
            writeExport(exportFile.outputStream(), records, format)
            currentCoroutineContext().ensureActive()
            val shareFailure = storage.share(exportFile, spec.mimeType).exceptionOrNull()
            if (shareFailure != null) {
                if (shareFailure is CancellationException) throw shareFailure
                deleteExportFile(exportFile, shareFailure)
                Result.failure(shareFailure)
            } else {
                storage.scheduleDeletion(exportFile)
                Result.success(Unit)
            }
        } catch (cancelled: CancellationException) {
            exportFile?.let { deleteExportFile(it, cancelled) }
            throw cancelled
        } catch (failure: Exception) {
            exportFile?.let { deleteExportFile(it, failure) }
            Result.failure(failure)
        }
    }

    private suspend fun exportToDocument(
        uri: String,
        records: List<SanitizedLogRecord>,
        format: LogFormat,
    ): Result<Unit> {
        return try {
            currentCoroutineContext().ensureActive()
            val output = storage.openDocument(uri)
            currentCoroutineContext().ensureActive()
            writeExport(output, records, format)
            currentCoroutineContext().ensureActive()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            deleteDocumentAfterFailure(uri, cancelled)
            throw cancelled
        } catch (failure: Throwable) {
            deleteDocumentAfterFailure(uri, failure)
            Result.failure(failure)
        }
    }

    private fun deleteExportFile(file: File, failure: Throwable) {
        try {
            storage.delete(file)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
    }

    private fun deleteDocumentAfterFailure(uri: String, failure: Throwable) {
        try {
            storage.deleteDocument(uri)
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
    }

    private fun writeExport(
        output: OutputStream,
        records: List<SanitizedLogRecord>,
        format: LogFormat,
    ) {
        exportEncoder.write(
            format = format,
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
        DiagnosticsExportFileRetention.staleFiles(
            context.cacheDir.listFiles(),
            System.currentTimeMillis(),
        ).forEach(File::delete)
    }

    override fun createExportFile(spec: ExportArtifactSpec): File = File(
        context.cacheDir,
        "${DiagnosticsExportFileRetention.PREFIX}${System.currentTimeMillis()}.${spec.extension}",
    )

    override fun share(file: File, mimeType: String): Result<Unit> = runCatching {
        require(file.isFile && file.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
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
        const val DELETE_DELAY_MS = DiagnosticsExportFileRetention.MAX_AGE_MS
    }
}
