package com.aozijx.passly.domain.clipboard.port

/** Writes a sensitive value using the active clipboard retention policy. */
fun interface SensitiveClipboardWriter {
    suspend fun writeSensitive(text: String)
}