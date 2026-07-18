package com.aozijx.passly.core.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File

object DiagnosticsExportManager {
    private const val EXPORT_PREFIX = "passly_diagnostics_"
    private const val DELETE_DELAY_MS = 10 * 60 * 1000L

    fun createPlaintextExport(context: Context): File {
        clearExports(context)
        val target = File(context.cacheDir, "$EXPORT_PREFIX${System.currentTimeMillis()}.txt")
        target.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            DiagnosticsRuntime.readAll().forEach(writer::appendLine)
        }
        return target
    }

    fun share(context: Context, file: File) {
        require(file.isFile && file.parentFile == context.cacheDir) { "Invalid diagnostics export" }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享诊断日志").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        Handler(Looper.getMainLooper()).postDelayed({ file.delete() }, DELETE_DELAY_MS)
    }

    fun clearExports(context: Context) {
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(EXPORT_PREFIX) }
            ?.forEach(File::delete)
    }
}
