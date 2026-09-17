package com.aozijx.passly.feature.settings.diagnostics

/** Feature-facing access to the persisted diagnostics log. */
interface DiagnosticsLogStore {
    suspend fun readLines(limit: Int): List<String>
    suspend fun clear()
}
