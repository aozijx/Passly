package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.BackupPreferences
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.ExportFormat
import com.aozijx.passly.domain.settings.model.ImportMode

// -- ExportFormat --
internal fun String.toExportFormatDomain(): ExportFormat = when (this) {
    "passly.encrypted" -> ExportFormat.ENCRYPTED
    "csv" -> ExportFormat.CSV
    "json" -> ExportFormat.JSON
    else -> ExportFormat.ENCRYPTED
}

internal fun ExportFormat.toExportFormatString(): String = when (this) {
    ExportFormat.ENCRYPTED -> "passly.encrypted"
    ExportFormat.CSV -> "csv"
    ExportFormat.JSON -> "json"
}

// -- ImportMode --
internal fun String.toImportModeDomain(): ImportMode = when (this) {
    "append" -> ImportMode.APPEND
    "replace" -> ImportMode.REPLACE
    "merge" -> ImportMode.MERGE
    else -> ImportMode.APPEND
}

internal fun ImportMode.toImportModeString(): String = when (this) {
    ImportMode.APPEND -> "append"
    ImportMode.REPLACE -> "replace"
    ImportMode.MERGE -> "merge"
}

internal fun readBackup(p: BackupPreferences): BackupSettings =
    BackupSettings(
        directoryTreeUri = p.directoryTreeUri.ifEmpty { null },
        defaultExportFormat = p.defaultExportFormat.toExportFormatDomain(),
        includeIcons = p.includeIcons,
        includeAttachments = p.includeAttachments,
        includeDeletedEntries = p.includeDeletedEntries,
        includedEntryTypes = p.includedEntryTypesList.toSet(),
        defaultImportMode = p.defaultImportMode.toImportModeDomain()
    )
