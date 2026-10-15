package com.aozijx.passly.feature.settings.diagnostics

enum class LogFormat(val extension: String, val mimeType: String) {
    JSONL("jsonl", "application/x-ndjson"),
    TSV("tsv", "text/tab-separated-values"),
    ZIP("zip", "application/zip"),
}

enum class ExportAction { SHARE, EXPORT_LOCAL }

data class ExportArtifactSpec(
    val extension: String,
    val mimeType: String,
)

fun LogFormat.artifactSpec(): ExportArtifactSpec = ExportArtifactSpec(extension, mimeType)
