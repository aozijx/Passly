package com.aozijx.passly.feature.backup.internal.model

import com.aozijx.passly.feature.backup.contract.BackupExportFormat

internal fun BackupExportFormat.toFormatId(): BackupFormatId =
    when (this) {
        BackupExportFormat.ENCRYPTED -> BackupFormats.PASSLY_ENCRYPTED
        BackupExportFormat.JSON -> BackupFormats.PASSLY_JSON
        BackupExportFormat.TEXT -> BackupFormats.READABLE_TEXT
    }
