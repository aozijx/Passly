package com.aozijx.passly.app.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticated diagnostics export adapter.
 *
 * The encrypted store stays in data/diagnostics; Android sharing and temporary plaintext lifetime
 * belong to the application boundary. Callers must authenticate before creating an export.
 */
@Singleton
class DiagnosticsExportService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtime: DiagnosticsRuntimeController
) {
    fun createPlaintextExport(): File {
        clearExports()
        val target = File(
            context.cacheDir,
            "$EXPORT_PREFIX${System.currentTimeMillis()}.txt"
        )
        target.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            runtime.readLines(EXPORT_LINE_LIMIT).forEach(writer::appendLine)
        }
        return target
    }

    fun share(file: File) {
        require(file.isFile && file.parentFile == context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Handler(Looper.getMainLooper()).postDelayed({ file.delete() }, DELETE_DELAY_MS)
    }

    private fun clearExports() {
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(EXPORT_PREFIX) }
            ?.forEach(File::delete)
    }

    private companion object {
        const val EXPORT_PREFIX = "passly_diagnostics_"
        const val EXPORT_LINE_LIMIT = 5_000
        const val DELETE_DELAY_MS = 10 * 60 * 1000L
    }
}
