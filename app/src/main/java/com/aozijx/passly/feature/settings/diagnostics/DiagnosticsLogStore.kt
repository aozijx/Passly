package com.aozijx.passly.feature.settings.diagnostics

/** Feature-facing access to the persisted diagnostics log. */
interface DiagnosticsLogStore {
    fun readLines(limit: Int): List<String>
    fun clear()
}