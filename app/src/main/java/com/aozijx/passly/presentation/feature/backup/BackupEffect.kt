package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat

internal sealed interface BackupEffect {
    data object SelectImportDocument : BackupEffect
    data class SelectExportDocument(
        val format: BackupExportFormat,
        val fileName: String,
    ) : BackupEffect
}

internal sealed interface BackupNoticeEffect : BackupEffect {
    val operation: BackupOperation

    data class Succeeded(
        override val operation: BackupOperation,
    ) : BackupNoticeEffect

    data class Failed(
        override val operation: BackupOperation,
    ) : BackupNoticeEffect
}
