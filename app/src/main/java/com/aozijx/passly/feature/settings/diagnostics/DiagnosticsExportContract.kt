package com.aozijx.passly.feature.settings.diagnostics

enum class LogFormat(val extension: String, val mimeType: String) {
    JSONL("jsonl", "application/x-ndjson"),
    TSV("tsv", "text/tab-separated-values"),
    ZIP("zip", "application/zip"),
}

enum class ExportAction { SHARE, EXPORT_LOCAL }

data class DiagnosticsExportRequest(
    val action: ExportAction,
    val format: LogFormat,
    val documentUri: String? = null,
) {
    init {
        when (action) {
            ExportAction.SHARE -> require(documentUri == null) { "Share request cannot have a document URI" }
            ExportAction.EXPORT_LOCAL -> require(!documentUri.isNullOrBlank()) {
                "Local export requires a document URI"
            }
        }
    }
}

data class ExportArtifactSpec(
    val extension: String,
    val mimeType: String,
)

fun LogFormat.artifactSpec(): ExportArtifactSpec = ExportArtifactSpec(extension, mimeType)
