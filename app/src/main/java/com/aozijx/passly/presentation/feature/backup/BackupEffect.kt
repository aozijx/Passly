package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult

internal sealed interface BackupEffect {
    data object SelectImportDocument : BackupEffect
    data class SelectExportDocument(
        val format: BackupExportFormat,
        val fileName: String,
    ) : BackupEffect
}

internal sealed interface BackupNoticeEffect : BackupEffect {
    data object DirectoryWritable : BackupNoticeEffect
    data object ExportSucceeded : BackupNoticeEffect
    data class ImportSucceeded(val result: BackupImportResult) : BackupNoticeEffect

    data class Failed(
        val operation: BackupOperation,
    ) : BackupNoticeEffect
}
