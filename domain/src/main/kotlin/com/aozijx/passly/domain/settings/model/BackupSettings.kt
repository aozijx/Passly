package com.aozijx.passly.domain.settings.model

data class BackupSettings(
    val directoryTreeUri: String? = null,
    val defaultExportFormat: ExportFormat = ExportFormat.ENCRYPTED,
    val includeIcons: Boolean = true,
    val includeAttachments: Boolean = true,
    val includeDeletedEntries: Boolean = true,
    val includedEntryTypes: Set<String> = emptySet(),
    val defaultImportMode: ImportMode = ImportMode.APPEND
)

enum class ExportFormat { ENCRYPTED, CSV, JSON }
enum class ImportMode { APPEND, REPLACE, MERGE }
