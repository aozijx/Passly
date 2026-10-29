package com.aozijx.passly.app.observability

import java.io.File

internal object DiagnosticsExportFileRetention {
    const val PREFIX = "passly_logs_"
    const val MAX_AGE_MS = 10 * 60 * 1000L

    fun staleFiles(files: Array<File>?, nowMs: Long): List<File> = files.orEmpty().filter { file ->
        if (!file.isFile || !file.name.startsWith(PREFIX)) return@filter false
        val modifiedAt = file.lastModified()
        nowMs >= modifiedAt && nowMs - modifiedAt >= MAX_AGE_MS
    }
}
