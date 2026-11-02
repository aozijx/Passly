package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperation

internal sealed interface BackupEffect {
    data class SelectImportDocument(
        val requestId: String,
    ) : BackupEffect

    data class SelectExportDocument(
        val format: BackupExportFormat,
        val fileName: String,
        val requestId: String,
    ) : BackupEffect
}

internal sealed interface BackupNoticeEffect : BackupEffect {
    data object DirectoryWritable : BackupNoticeEffect

    data object ExportSucceeded : BackupNoticeEffect

    data class ImportSucceeded(
        val result: BackupImportResult,
    ) : BackupNoticeEffect

    data class Failed(
        val operation: BackupOperation,
    ) : BackupNoticeEffect
}
